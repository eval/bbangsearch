(ns bbangsearch.main
  (:require [babashka.cli :as cli]
            [babashka.process :refer [process]]
            [bbangsearch.bangs :as bangs]
            [bbangsearch.util :as util]
            [clojure.java.browse :refer [browse-url]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [selmer.parser :as selmer]))

(defn- compile-ifmatches-args [[expr re :as args] context-map]
  #_(prn ::compile-ifmatches-args :args args :context-map context-map)
  (let [matchee (selmer/render (str "{{" expr "}}") context-map)
        matcher (read-string re)]
    (when-not (or (instance? java.util.regex.Pattern matcher)
                  (= 2 (count args)))
      (throw (ex-info (str "invalid arguments passed to 'ifmatches' tag: " args \newline
                           "Example: {% ifmatches some-var #\"foo\" %} ")
                      {:args args})))
    [matchee matcher]))

(selmer/add-tag!
 :ifmatches
 (fn [& [args context-map content :as m]]
   #_(prn :content content)
   (let [negate?           (= "not" (first args))
         args              (if negate? (rest args) args)
         [matchee matcher] (compile-ifmatches-args args context-map)
         body              (get-in content [:ifmatches :content])
         pred              (if negate? (complement re-find) re-find)]
     (if (pred matcher matchee)
       body
       (-> content :else :content))))
 :else :endifmatches)

(def ^:private  current-file *file*)

(defn- git [& args]
  (some-> (apply process "git" args) :out slurp string/trimr))

(defn- github-user []
  (or (System/getenv "BBANG_GITHUB_USER")
      (System/getenv "GITHUB_USER")
      (git "config" "--get" "github.user")))

(defn- github-org []
  (or
   (System/getenv "BBANG_GITHUB_ORG")
   (System/getenv "GITHUB_ORG")
   (git "config" "--get" "github.org")
   (github-user)))

(defn- current-version []
  (let [dev?    (nil? (io/resource "VERSION"))
        bin     (if dev? "bbang-dev" "bbang")
        version (string/trim
                 (if dev?
                   (let [git-dir (util/find-up ".git" current-file)]
                     (git "--git-dir" (str git-dir) "describe" "--tags"))
                   (slurp (io/resource "VERSION"))))]
    (str bin " " version)))

(defn- print-version []
  (println (current-version)))

(defn- bang-url [bang vars]
  (selmer/render bang vars))

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

(def ^:private vars {'github-org #'github-org})

(defn- handle-bang [requested-bang bang-args cli-opts]
  (if-let [{bang-tpl :tpl :as bang} (get (bangs/all) requested-bang)]
    (let [known-vars    (keys vars)
          required-vars (some-> bang meta :vars)
          unknown-vars  (remove (set known-vars) required-vars)
          _             (when (seq unknown-vars)
                          (throw (ex-info (str "Unknown vars: " (pr-str unknown-vars) ". Valid vars: " (pr-str known-vars)) {})))
          vars          (update-keys (update-vals (select-keys vars required-vars)
                                                  #(apply % '())) keyword)
          s             (quote-bang-args bang-args)
          url           (string/trim (bang-url bang-tpl (merge vars {:s (str s)})))]
      (cond
        (:url cli-opts) (println url)
        :else           (browse-url url)))
    (do (println (str "Unknown bang '" requested-bang "'."))
        (System/exit 1))))

(defn- handle-bangs-ls [_ls-args cli-opts]
  (print-bangs (printable-bangs (bangs/all)) cli-opts))

(defn- handle-help []
  (println (str (current-version) "

A CLI for DuckDuckGo's bang searches written in Babashka.

"
                  (util/bold  "USAGE" {}) "
  $ bbang [bang [terms] [--url]]
  $ bbang [COMMAND]

"
                  (util/bold "OPTIONS" {}) "
  --url  Print url instead of opening browser.

"
                  (util/bold "COMMANDS" {}) "
  bangs:ls  List all bangs (or via `bbang bangs <terms>`)
")))

(defn -main [& args]
  (let [{[cmd & cmd-args :as _parsed-args] :args
         :keys                             [opts]} (cli/parse-args args {:restrict [:version :help :url]
                                                                         :alias    {:h :help :v :version}})]
    #_(prn :cmd cmd :cmd-args cmd-args #_#_:parsed-args parsed-args)
    (cond
      (or (empty? args) (:help opts)) (handle-help)
      (:version opts)                 (print-version)
      (= cmd "bangs:ls")              (handle-bangs-ls cmd-args opts)
      :else                           (handle-bang cmd cmd-args opts))))

(comment

  #_:end)
