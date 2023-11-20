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

(defn ddg-bangs []
  (load-edn (io/resource "bangs.edn")))

(def additional-bangs
  {"cljdoc"    {:aliases ["cljd"]}
   "ghclj"     {:desc "GitHub Clojure projects"
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
   "ghrepo"    {:desc "Search/visit (current) repos on GitHub"
                :tpl  "{% if s|empty? %}https://github.com/{{org&project}}{% else %}https://github.com/search?q=repo%3A{{org&project|urlescape}}%20{{s|urlescape}}&type=code{% endif %}"}
   "ghdbf"     {:desc "GitHub dashboard feed"
                :tpl  "https://github.com/dashboard-feed"}
   "ghrel"     {:desc "Search/visit GitHub releases of (current) repo"
                :tpl  "{% if s|empty? %}https://github.com/{{org&project}}/releases{% else %}https://github.com/{{org&project}}/releases?q={{s|urlescape}}&expanded=true{% endif %}"}
   "tldrlegal" {:desc "TL;DR Legal" ;; fixes default
                :tpl  "https://www.tldrlegal.com/search?query={{s|urlescape}}"}
   "drtv"      {:desc "DR TV"
                :tpl  "https://www.dr.dk/drtv/soeg?q={{s|urlescape}}"}
   "drdk"      {:desc "Danmark Radio"
                :tpl  "https://www.dr.dk/soeg?query={{s|urlescape}}&sort=Relevance"}
   "java19"    {:desc "Java19 docs"
                :tpl  "https://docs.oracle.com/en/java/javase/19/docs/api/search.html?q={{s|urlescape}}"}
   "java20"    {:desc "Java20 docs"
                :tpl  "https://docs.oracle.com/en/java/javase/20/docs/api/search.html?q={{s|urlescape}}"}
   "java21"    {:desc    "Java21 docs"
                :aliases ["java"]
                :tpl     "https://docs.oracle.com/en/java/javase/21/docs/api/search.html?q={{s|urlescape}}"}
   "pgdoc14"   {:desc "Postgresql docs (v14)"
                :tpl  "https://www.postgresql.org/search/?u=%2Fdocs%2F14%2F&q={{s|urlescape}}"}
   "pgdoc15"   {:desc "Postgresql docs (v15)"
                :tpl  "https://www.postgresql.org/search/?u=%2Fdocs%2F15%2F&q={{s|urlescape}}"}
   "pgdoc16"   {:desc "Postgresql docs (v16)"
                :tpl  "https://www.postgresql.org/search/?u=%2Fdocs%2F16%2F&q={{s|urlescape}}"}
   "pgdoc"     {:desc "Postgresql docs (current version)"
                :tpl  "https://www.postgresql.org/search/?u=%2Fdocs%2Fcurrent%2F&q={{s|urlescape}}"}
   "rails61"   {:desc    "Rails API v6.1.x"
                :tpl     "https://api.rubyonrails.org/v6.1?q={{s|urlescape}}"
                :aliases ["rails6"]}
   "rails70"   {:desc "Rails API v7.0.x"
                :tpl  "https://api.rubyonrails.org/v7.0?q={{s|urlescape}}"}
   "rails71"   {:desc    "Rails API v7.1.x"
                :tpl     "https://api.rubyonrails.org/v7.1?q={{s|urlescape}}"
                :aliases ["rails7"]}})

(defn- merge-bang-maps [a b]
  (let [aliases (into (:aliases a) (:aliases b))]
    (assoc (merge a b) :aliases aliases)))

(defn built-in-bangs []
  (merge-with merge-bang-maps (ddg-bangs) additional-bangs))

(defn- extract-aliases [m]
  (reduce (fn [acc [k {:keys [aliases]}]]
            (into acc (map vector aliases (repeat k))))
          {}
          (filter (comp :aliases val) m)))

(defn user-config-bangs []
  (util/ensure-path-exists! @config-home)
  (when-let [bangs-edn (util/file-exists?-> @config-home "bangs.edn")]
    (load-edn bangs-edn)))

(defn folder-bang-maps []
  (let [files (keep #(util/file-exists?-> % "bangs.edn") (util/traverse-up))]
    (map load-edn (reverse files))))

(defn user-bang-maps
  "All bang maps from user, least important first, e.g. '(user-config home-folder project-folder)"
  []
  (filter map? (conj (folder-bang-maps) (user-config-bangs))))

(defn- alias->bang-name
  "Turn bang maps into one alias map"
  [ms]
  (reduce merge (map extract-aliases ms)))

(defn all []
  (let [bang-maps      (conj (user-bang-maps) (built-in-bangs))
        setify-aliases (fn [m] (update-vals m #(update % :aliases set)))
        bang-maps      (map setify-aliases bang-maps)
        aliases        (alias->bang-name bang-maps)
        ;; e.g. a 'prio-alias' is "rails" in {"rails" "project/rails" "r" "rails"}
        ;; or else "r" would point to the standard rails and not the project rails
        prio-aliases   (set (filter (set (keys aliases)) (vals aliases)))
        aliases        (into (filterv (comp prio-aliases key) aliases) aliases)
        result         (apply merge-with merge-bang-maps bang-maps)]
    (reduce (fn [acc [al bang]]
              (let [alias-bang (-> acc
                                   (get bang)
                                   ;; remove itself from this copy
                                   (update :aliases #(disj % al))
                                   ;; add reverse alias
                                   (update :aliases #(conj % bang)))]
                (-> (assoc acc al alias-bang)
                    (update-in [bang :aliases] #(conj % al))))) result aliases)))

(defn find [bang]
  (get (all) bang))

(comment
  (find "moar")

  (set (filter (comp #{"foo"} key) {"foo" :1 "bar" :2}))
  (let [aliases {"rails" "project/rails" "r" "rails"}
        prio-aliases (set (filter (set (keys aliases)) (vals aliases)))]
    (into (filterv (comp prio-aliases key) aliases) aliases))

  ((fn fuzzy-find [s]
     (let [fuzzy-re  (re-pattern (apply str "^" (interpose ".*" s)))]
       (if-let [result (get (all) s)]
         result
         (filter #(re-find fuzzy-re %) (keys (all)))))) "cljd")

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
