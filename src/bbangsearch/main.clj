(ns bbangsearch.main
  (:require [babashka.cli :as cli]
            [bbangsearch.bangs :as bangs]
            [bbangsearch.util :as util]
            [clojure.java.browse :refer [browse-url]]
            [clojure.string :as string]
            [selmer.parser :as selmer]))

(defn- bang-url [bang s]
  (selmer/render bang {:s (str s)}))

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
        #_#__         (prn :max max-width)
        desc-truncate #(util/truncate %1 {:truncate-to %2
                                          :omission    "…"})]
    (util/print-table column-atts printable-scripts {:skip-header          skip-header?
                                                     :max-width            max-width
                                                     :width-reduce-column  :desc
                                                     :width-reduce-fn      desc-truncate
                                                     #_#_:column-coercions column-coercions
                                                     :no-color             no-color?})))

(defn- quote-bang-args [bang-args]
  (string/join " "
               (map (fn [term]
                      (if (string/includes? term " ")
                        (pr-str term)
                        term)) bang-args)))


(defn- handle-bang [requested-bang bang-args cli-opts]
  (if-let [{bang-tpl :tpl :as _bang} (get (bangs/all) requested-bang)]
    (let [q   (quote-bang-args bang-args)
          url (string/trim (bang-url bang-tpl q))]
      (cond
        (:url cli-opts) (println url)
        :else           (browse-url url)))
    (do (println (str "Unknown bang '" requested-bang "'."))
        (System/exit 1))))

(defn- handle-bangs-ls [_ls-args cli-opts]
  (print-bangs (printable-bangs (bangs/all)) cli-opts))

(defn- handle-help []
  (println (str "BBangSearch

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
  bangs:ls  List all bangs (or via `bs bangs <terms>`)
")))
#_((fn [& args]
   (let [{[cmd & cmd-args :as parsed-args] :args
          :keys [opts]} (doto (cli/parse-args args {:alias {:h :help}}) prn)]
     (prn :cmd cmd :cmd-args cmd-args #_#_:parsed-args parsed-args)
     (cond
       (or (empty? args) (:help opts)) :help
       (= cmd "bangs:ls") :bangs-ls
       :else :bang))) "bangs:ls" "-e" "foo")

#_(def ^:private cli-opts {:alias {:h :help}})
;; '("-h" "--help" "help")
;; "bangs:ls"
;; '("some-bang") '("some-bang" "some-query")
(defn -main [& args]
  (let [{[cmd & cmd-args :as _parsed-args] :args
         :keys                            [opts]} (cli/parse-args args {:alias {:h :help}})]
    #_(prn :cmd cmd :cmd-args cmd-args #_#_:parsed-args parsed-args)
    (cond
      (or (empty? args) (:help opts)) (handle-help)
      (= cmd "bangs:ls")              (handle-bangs-ls cmd-args opts)
      :else                           (handle-bang cmd cmd-args opts)))

  #_(let [parsed-args (cli/parse-args args cli-opts)]
      #_(print-bangs (printable-bangs (bangs)) {})
      #_(let [[bang-arg & bang-q] args
              q                   (string/join " "
                                               (map (fn [term]
                                                      (if (string/includes? term " ")
                                                        (pr-str term)
                                                        term)) bang-q))]
          #_(prn :arg args :q q)
          (if-let [{tpl :tpl} (get (bangs) bang-arg)]
            (browse-url (doto (bang-url tpl q) prn))
            (do (println (str "Unknown bang '" (first args) "'."))
                (System/exit 1))))))

(comment

  #_:end)
