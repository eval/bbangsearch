(ns bbangsearch.bangs
  (:refer-clojure :exclude [find])
  (:require [babashka.fs :as fs] :reload
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [bbangsearch.util :as util]))

(def ^:private config-home (delay (fs/xdg-config-home "bbang")))

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

(defn- ensure-domain
  ([bang] (ensure-domain bang nil))
  ([bang domain]
   (let [bang->domain #(some->> % :tpl (re-find #"https?:\/\/([^/]+)") second)]
     (assoc bang :domain (or domain (bang->domain bang))))))

(defn default []
  (load-edn (io/resource "bangs.edn")))

(defn additional []
  (-> {"ghclj"     {:desc "GitHub Clojure projects"
                    :tpl  "https://github.com/search?utf8=%E2%9C%93&q={{s|urlescape}}+language%3AClojure&type=repositories&l=Clojure"}
       "ghcclj"    {:desc "GitHub Clojure code"
                    :tpl  "https://github.com/search?utf8=%E2%9C%93&q={{s|urlescape}}+language%3AClojure&type=code"}
       "grep"      {:desc "Grep.app"
                    :tpl  "https://grep.app/search?q={{s|urlescape}}"}
       "grepclj"   {:desc "Grep.app Clojure code"
                    :tpl  "https://grep.app/search?q={{s|urlescape}}&filter[lang][0]=Clojure"}
       "rdoc"      {:desc "rubydoc.info, gems only (jump-to: @gem)" ;; fixes default
                    :tpl  "{% ifmatches #\"^@\" s %}https://www.rubydoc.info/gems/{{s|drop:1|join}}{% else %}https://www.rubydoc.info/find/gems?q={{s|urlescape}}{% endifmatches %}"}
       "@rdoc"     {:dec "Jump to gem on rubydoc.info"
                    :tpl "https://www.rubydoc.info/gems/{{s}}"}
       "@gem"      {:desc "Jump to gem on rubygems.org"
                    :tpl  "https://rubygems.org/gems/{{s}}"}
       "gem"       {:desc "RubyGems (jump-to: @gem)"
                    :tpl  "{% ifmatches #\"^@\" s %}https://rubygems.org/gems/{{s|drop:1|join}}{% else %}https://rubygems.org/search?utf8=%E2%9C%93&query={{s|urlescape}}{% endifmatches %}"}
       "ghrepo"    {:desc "Search/visit repo from working dir. See README."
                    :tpl  "{% if s|empty? %}https://github.com/{{org&project}}{% else %}https://github.com/search?q=repo%3A{{org&project|urlescape}}%20{{s|urlescape}}&type=code{% endif %}"}
       "ghdbf"     {:desc "GitHub dashboard feed"
                    :tpl  "https://github.com/dashboard-feed"}
       "ghrel"     {:desc "GitHub releases"
                    :tpl  "{% if s|empty? %}https://github.com/{{org&project}}/releases{% else %}https://github.com/{{org&project}}/releases?q={{s|urlescape}}&expanded=true{% endif %}"}
       "tldrlegal" {:desc "TL;DR Legal" ;; fixes default
                    :tpl  "https://www.tldrlegal.com/search?query={{s|urlescape}}"}}
      (update-vals ensure-domain)))

(defn user []
  (util/ensure-path-exists! @config-home)
  (when-let [bangs-edn (util/when-pred fs/exists? (fs/file @config-home "bangs.edn"))]
    (update-vals (load-edn bangs-edn)
                 ensure-domain)))

(defn all []
  (merge (default) (additional) (user)))

(defn find [bang]
  (get (all) bang))

(comment

  (let [bangs-sans-urlescape #{"ghrepo"}
        bangs-to-remove      #{"web1913"}
        urlify-paths         #(if (re-find #"^/" %)
                                (str "https://duckduckgo.com" %)
                                %)]
    (spit "resources/bangs.edn"
          (apply dissoc (reduce (fn [acc {desc :s tpl :u bang :t domain :d}]
                                  (let [tpl-replacement (if (bangs-sans-urlescape bang) "{{s}}" "{{s|urlescape}}")
                                        tpl             (-> tpl
                                                            (string/replace #"\{\{\{s\}\}\}" tpl-replacement)
                                                            urlify-paths)
                                        m               (ensure-domain {:desc desc :tpl tpl :bang bang} domain)]
                                    (assoc acc bang m))) {} (load-edn (io/resource "bangs.org.edn")))
                 bangs-to-remove)))

  #_:end)
