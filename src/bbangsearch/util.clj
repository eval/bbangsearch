(ns bbangsearch.util
  (:require [babashka.fs :as fs] :reload
            [babashka.process :as p]
            [clojure.string :as string]))

(defn traverse-up
  ([] (traverse-up (fs/cwd)))
  ([p]
   (let [p (fs/normalize (fs/absolutize (fs/path p)))]
     (take-while some? (iterate fs/parent (if (fs/directory? p) p (fs/parent p)))))))

(defn ensure-path-exists! [path]
  (fs/create-dirs path))

(defn when-pred
  "Examples:
  (when-pred even? 2) ;; => 2"
  [pred val]
  (when (pred val) val))

(defn file-exists?-> [& fs]
  (when-pred fs/exists? (apply fs/file fs)))

(defn git [& args]
  (some-> (apply p/process "git" args) :out slurp string/trimr))

(defn is-tty
  [fd key]
  (-> ["test" "-t" (str fd)]
      (p/process {key :inherit :env {}})
      deref
      :exit
      (= 0)))

(def tty-out? (memoize #(is-tty 1 :out)))

(defn terminal-dimensions
  "Yields e.g. `{:cols 30 :rows 120}`"
  []
  (->
   (p/process ["stty" "size"] {:inherit true :out :string})
   deref
   :out
   string/trim
   (string/split #" ")
   (->> (map #(Integer/parseInt %))
        (zipmap [:rows :cols]))))

(defn truncate
  "Truncates `s` when it exceeds length `truncate-to` by inserting `omission` at the given `omission-position`.

  The result's length will equal `truncate-to`, unless `truncate-to` < `omission`-length, in which case the result equals `omission`.

  Examples:
  ```clojure
  (truncate \"1234567\" {:truncate-to 7})
  # => \"1234567\"

  (truncate \"1234567\" {:truncate-to 5})
  # => \"12...\"

  (truncate \"1234567\" {:truncate-to 5 :omission \"(continued)\"})
  # => \"(continued)\"

  (truncate \"example.org/path/to/release/v1.2.3/server.jar\"
    {:omission \"…\" :truncate-to 35 :omission-position :center})
  # => \"example.org/path/…v1.2.3/server.jar\"
  ```

  Options:
  - `truncate-to` (`30`) length above which truncating will occur. The resulting string will have this length (assuming `(> truncate-to (count omission))`).
  - `omission` (`\"...\"`) what to use as omission.
  - `omission-position` (`:end`) where to put omission. Options: `#{:center :end}`.
  "
  [s {:keys [omission truncate-to omission-position]
      :or   {omission "..." truncate-to 30 omission-position :end}}]
  (if-not (> (count s) truncate-to)
    s
    (let [truncated-s-length  (max 0 (- truncate-to (count omission)))
          [lsub-len rsub-len] (case omission-position
                                :end    [truncated-s-length 0]
                                :center (if (even? truncated-s-length)
                                          [(/ truncated-s-length 2) (/ truncated-s-length 2)]
                                          [(/ (inc truncated-s-length) 2) (/ (dec truncated-s-length) 2)]))]
      (str (subs s 0 lsub-len)
           omission
           (subs s (- (count s) rsub-len) (count s))))))

(defn plain-mode? [{:keys [plain] :as _cli-opts}]
  (or (fs/windows?) plain (not (tty-out?))))

(defn no-color? [{:keys [color] :as cli-opts}]
  (or (false? color)
      (plain-mode? cli-opts)
      (System/getenv "NO_COLOR")
      (= "dumb" (System/getenv "TERM"))))

(defn bold [s cli-opts]
  (if (no-color? cli-opts) s (str "\033[1m" s "\033[0m")))

(defn print-table
  "Print table to stdout.

  Examples:
  ```clojure
  ;; Extract columns from rows
  (print-table [{:a \"one\" :b \"two\"}])

  a    b
  ───  ───
  one  two

  ;; Provide columns (as b is an empty column, it will be skipped)
  (print-table [:a :b] [{:a \"one\" :b nil}])

  a
  ───
  one

  ;; Ensure all columns being shown:
  (print-table [:a :b] [{:a \"one\"}] {:show-empty-columns true})

  ;; Provide columns with labels and apply column coercion
  (print-table {:a \"option A\" :b \"option B\"} [{:a \"one\" :b nil}]
               {:column-coercions {:b (fnil boolean false)}})

  option A  option B
  ────────  ────────
  one       false

  ;; Provide `max-width` and `:width-reduce-column` to try to make the table fit smaller screens.
  (print-table {:a \"123456\"} {:max-width 5 :width-reduce-column :a})

  a
  ─────
  12...

  ;; A custom `width-reduce-fn` can be provided. See options for details.
  (print-table {:a \"123456\"} {:max-width 5
                                :width-reduce-column :a
                                :width-reduce-fn #(subs %1 0 %2)})
  a
  ─────
  12345

  ```

  Options:
  - `column-coercions` (`{}`) fn that given a key `k` yields an fn to be applied to every `(k row)` *iff* row contains key `k`.
    See example above.
  - `skip-header` (`false`) don't print column names and divider (typically use this when stdout is no tty).
  - `show-empty-columns` (`false`) print every column, even if it results in empty columns.
  - `no-color` (`false`) prevent printing escape characters to stdout.
  - `max-width` (`nil`) when width of the table exceeds this value, `width-reduce-fn` will be applied to all cells of column `width-reduce-column`. NOTE: providing this, requires `width-reduce-column` to be provided as well.
  - `width-reduce-column` (`nil`) column that `width-reduce-fn` will be applied to when table width exceeds `max-width`.
  - `width-reduce-fn` (`#(truncate %1 {:truncate-to %2})`) function that is applied to all cells of column `width-reduce-column` when the table exceeds width `max-width`.
    The function should have 2-arity: a string (representing the cell value) and an integer (representing the max size of the cell contents in order for the table to stay within `max-width`)."
  ([rows]
   (print-table rows {}))
  ([ks-rows rows-opts]
   (let [rows->ks       #(-> % first keys)
         [ks rows opts] (if (map? rows-opts)
                          [(rows->ks ks-rows) ks-rows rows-opts]
                          [ks-rows rows-opts {}])]
     (print-table ks rows opts)))
  ([ks rows {:as   opts
             :keys [show-empty-columns skip-header no-color column-coercions
                    max-width width-reduce-column width-reduce-fn]
             :or   {show-empty-columns false skip-header false no-color false column-coercions {}}}]
   (assert (or (not max-width) (and max-width ((set ks) width-reduce-column)))
           (str "Option :max-width requires option :width-reduce-column to be one of " (pr-str ks)))
   (let [wrap-bold            (fn [s] (if no-color s (str "\033[1m" s "\033[0m")))
         row-get              (fn [row k]
                                (when (contains? row k)
                                  ((column-coercions k identity) (get row k))))
         key->label           (if (map? ks) ks #(subs (str (keyword %)) 1))
         header-keys          (if (map? ks) (keys ks) ks)
         ;; ensure all header-keys exist for every row and every value is a string
         rows                 (map (fn [row]
                                     (reduce (fn [acc k]
                                               (assoc acc k (str (row-get row k)))) {} header-keys)) rows)
         header-keys          (if show-empty-columns
                                header-keys
                                (let [non-empty-cols (remove
                                                      (fn [k] (every? string/blank? (map #(get % k) rows)))
                                                      header-keys)]
                                  (filter (set non-empty-cols) header-keys)))
         header-labels        (map key->label header-keys)
         column-widths        (reduce (fn [acc k]
                                        (let [val-widths (map count (cons (key->label k)
                                                                          (map #(get % k) rows)))]
                                          (assoc acc k (apply max val-widths)))) {} header-keys)
         row-fmt              (string/join "  " (map #(str "%-" (column-widths %) "s") header-keys))
         cells->formatted-row #(apply format row-fmt %)
         plain-header-row     (cells->formatted-row header-labels)
         required-width       (count plain-header-row)
         header-row           (wrap-bold plain-header-row)
         max-width-exceeded?  (and max-width
                                   (> required-width max-width))
         div-row              (wrap-bold
                               (cells->formatted-row
                                (map (fn [k]
                                       (apply str (take (column-widths k) (repeat \u2500)))) header-keys)))
         data-rows            (map #(string/trimr (cells->formatted-row (map % header-keys))) rows)]
     (if-not max-width-exceeded?
       (when (seq header-keys)
         (let [header (if skip-header (vector) (vector header-row div-row))]
           (println (apply str (interpose \newline (into header data-rows))))))
       (let [overflow         (- required-width max-width)
             max-column-width (max 0 (- (column-widths width-reduce-column) overflow))
             width-reduce-fn  (or width-reduce-fn #(truncate %1 {:truncate-to %2}))
             coercion-fn      #(width-reduce-fn % max-column-width)]
         (recur ks rows (assoc opts
                               :max-width nil
                               :column-coercions {width-reduce-column coercion-fn})))))))

(comment
  (print-table [{:a 1}])
  #_:end)
