(ns bbangsearch.main
  (:require [babashka.cli :as cli]
            [babashka.process :refer [process]]
            [bbangsearch.bangs :as bangs]
            [bbangsearch.util :as util]
            [clojure.java.browse :refer [browse-url]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [selmer.parser :as selmer]))

(defn- compile-ifmatches-args [[re expr :as args] context-map]
  #_(prn ::compile-ifmatches-args :args args :context-map context-map)
  (let [matchee (get context-map (keyword expr))
        matcher (read-string re)]
    (when-not (and (instance? java.util.regex.Pattern matcher)
                   (= 2 (count args)))
      (throw (ex-info (str "Invalid arguments passed to 'ifmatches' tag: " args \newline
                           "Example: {% ifmatches #\"foo\" some-var %},,,{% endifmatches %}")
                      {:args args})))
    [matcher matchee]))

(selmer/add-tag!
 :ifmatches
 (fn [& [args context-map content :as _m]]
   #_(prn :content content)
   (let [negate?           (= "not" (first args))
         args              (if negate? (rest args) args)
         [matcher matchee] (compile-ifmatches-args args context-map)
         body              (get-in content [:ifmatches :content])
         pred              (if negate? (complement re-find) re-find)]
     (if (pred matcher matchee)
       body
       (-> content :else :content))))
 :else :endifmatches)

(defn- throw-ex [msg]
  (throw (ex-info msg {})))

(defn- handle-ex [e]
  (println (ex-message e))
  (System/exit 1))

(def ^:private  current-file *file*)

(defn- git [& args]
  (some-> (apply process "git" args) :out slurp string/trimr))

(defn- github-user []
  (or (System/getenv "BBANG_GITHUB_USER")
      (System/getenv "GITHUB_USER")
      (not-empty (git "config" "--get" "github.user"))))

(defn- github-org []
  (or
   (System/getenv "BBANG_GITHUB_ORG")
   (System/getenv "GITHUB_ORG")
   (not-empty (git "config" "--get" "github.org"))
   (github-user)))

(defn- current-github-remote-url []
  (let [ssh-url?        (comp #(re-find #"^git@github" %) :url)
        github-url?     (comp #(re-find #"github\.com" %) :url)
        origin?         (comp #(= % "origin") :name)
        origin&ssh-url? (every-pred origin? ssh-url?)
        remote->url     #(git "remote" "get-url" %)
        remotes         (map #(assoc {} :name % :url (remote->url %))
                             (string/split-lines (git "remote")))]
    (some->> remotes
             (filter github-url?)
             (filter (some-fn origin&ssh-url? ssh-url? origin?))
             first
             :url)))

(defn- current-github-org&project []
  (some->> (current-github-remote-url) (re-find #"[:/]([^.]+)\.git$") second))

(defn- bbang-version []
  (let [dev?    (nil? (io/resource "VERSION"))
        bin     (if dev? "bbang-dev" "bbang")
        version (string/trim
                 (if dev?
                   (let [git-dir (util/find-up ".git" current-file)]
                     (git "--git-dir" (str git-dir) "describe" "--tags"))
                   (slurp (io/resource "VERSION"))))]
    (str bin " " version)))

(defn- print-version []
  (println (bbang-version)))

(defn- bang-url [bang-tpl vars]
  (selmer/render bang-tpl vars))

(defn- printable-bangs [bangs]
  (sort-by :bang (reduce (fn [acc [bang bang-cfg]]
                           (conj acc (assoc bang-cfg :bang bang))) [] bangs)))

(defn- print-bangs [printable-scripts cli-opts]
  (let [no-color?     (util/no-color? cli-opts)
        plain-mode?   (util/plain-mode? cli-opts)
        skip-header?  plain-mode?
        column-atts   '(:bang :domain :desc)
        max-width     (when-not plain-mode?
                        (:cols (util/terminal-dimensions)))
        desc-truncate #(util/truncate %1 {:truncate-to %2
                                          :omission    "…"})]
    (util/print-table column-atts printable-scripts {:skip-header          skip-header?
                                                     :max-width            max-width
                                                     :width-reduce-column  :desc
                                                     :width-reduce-fn      desc-truncate
                                                     :no-color             no-color?})))

(defn- quote-bang-args [bang-args]
  (string/join " "
               (map (fn [term]
                      (if (string/includes? term " ")
                        (pr-str term)
                        term)) bang-args)))

(defn- bang-vars-dispatch [bang _bang-args] bang)

(defmulti bang-vars #'bang-vars-dispatch)

(defmethod bang-vars "ghrepo" [_bang [org&project & terms]]
  (let [org&project (cond
                      (or
                       (nil? org&project)
                       (= org&project "_"))            (or (current-github-org&project)
                                                           (throw-ex "Can't establish the github-url of this project. Is there a git remote pointing to github?"))
                      (string/index-of org&project \/) org&project
                      :else
                      (if-let [org (github-org)]
                        (str org "/" org&project)
                        (throw-ex "Can't establish github-org. See https://github.com/search?q=repo%3Aeval%2Fbbangsearch%20%22set+github-org%22&type=code")))]
    (cond-> {:org&project org&project}
      (seq terms) (assoc :s (quote-bang-args terms)))))

(defmethod bang-vars :default [_bang bang-args]
  {:s (quote-bang-args bang-args)})

(defn- handle-bang [requested-bang bang-args cli-opts]
  #_(prn :bang-args bang-args :cli-opts cli-opts)
  (if-let [{bang-tpl :tpl :as _bang} (bangs/find requested-bang)]
    (let [vars (bang-vars requested-bang bang-args)
          url  (string/trim (bang-url bang-tpl vars))]
      (cond
        (:url cli-opts) (println url)
        :else           (browse-url url)))
    (throw-ex (str "Unknown bang '" requested-bang "'."))))

(defn- handle-bangs-ls [_ls-args cli-opts]
  (print-bangs (printable-bangs (bangs/all)) cli-opts))

(defn- handle-help []
  (println (str (bbang-version) "

A CLI for DuckDuckGo's bang searches written in Babashka.

"
                  (util/bold  "USAGE" {}) "
  $ bbang [bang [& terms] [--url]]
  $ bbang [COMMAND]

"
                  (util/bold "OPTIONS" {}) "
  --url  Print url instead of opening browser.

"
                  (util/bold "COMMANDS" {}) "
  bangs:ls  List all bangs (or via `bbang bangs [& terms]`)
")))

(defn -main [& args]
  (try
    (let [{[cmd & cmd-args :as _parsed-args] :args
           :keys                             [opts]} (cli/parse-args args {:restrict [:version :help :url]
                                                                           :alias    {:h :help :v :version}})]
      #_(prn :cmd cmd :cmd-args cmd-args #_#_:parsed-args parsed-args)
      (cond
        (or (empty? args) (:help opts)) (handle-help)
        (:version opts)                 (print-version)
        (= cmd "bangs:ls")              (handle-bangs-ls cmd-args opts)
        :else                           (handle-bang cmd cmd-args opts)))
    (catch Exception e
      (handle-ex e))))

(comment

  #_:end)
