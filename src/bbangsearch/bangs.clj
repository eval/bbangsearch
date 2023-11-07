(ns bbangsearch.bangs
  (:require [babashka.fs :as fs] :reload
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [bbangsearch.util :as util]))



(def ^:private config-home (delay (fs/xdg-config-home "bangsearch")))

;; SOURCE https://clojuredocs.org/clojure.edn/read#example-5a68f384e4b09621d9f53a79
(defn- load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))
    (catch java.io.IOException e
      (printf "Couldn't open '%s': %s\n" source (.getMessage e)))
    (catch RuntimeException e
      (printf "Error parsing edn file '%s': %s\n" source (.getMessage e)))))

(def additional
  {"ghclj"   {:desc "GitHub Clojure projects" :tpl "https://github.com/search?utf8=%E2%9C%93&q={{s|urlescape}}+language%3AClojure&type=repositories&l=Clojure"}
   "grep"    {:desc "Grep.app" :tpl "https://grep.app/search?q={{s|urlescape}}"}
   "grepclj" {:desc "Grep.app Clojure code" :tpl "https://grep.app/search?q={{s|urlescape}}&filter[lang][0]=Clojure"}})

(defn- assoc-domain [bang]
  (let [bang->domain #(some->> % :tpl (re-find #"https?:\/\/([^/]+)") second)]
    (assoc bang :domain (bang->domain bang))))

(defn user []
  (util/ensure-path-exists! @config-home)
  (when-let [bangs-edn (util/when-pred fs/exists? (fs/file @config-home "bangs.edn"))]
    (update-vals (load-edn bangs-edn)
                 assoc-domain)))

(defn all []
  (merge default additional (user)))


(comment

  (count default)
  (count (bangs))
  (update (update-in (dissoc (reduce (fn [acc {desc :s tpl :u bang :t domain :d}]
                                (let [tpl (string/replace tpl #"\{\{\{s\}\}\}" "{{s|urlescape}}")]
                                  (assoc acc bang {:domain domain :desc desc :tpl tpl}))) {} (load-edn (io/resource "bangs.edn")))
                             "web1913")
                     ["bang" :tpl] #(str "https://duckduckgo.com" %))
          "bang" assoc-domain)

  #_:end)