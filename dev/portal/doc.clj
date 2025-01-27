(ns portal.doc
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [examples.data :as d]
            [portal.api :as p]
            [portal.viewer :as-alias v]))

(def info
  {::v/log
   {:file "portal/ui/viewer/log.cljs"
    :examples d/log-data}
   ::v/prepl
   {:file "portal/ui/viewer/prepl.cljs"
    :examples [d/prepl-data]}
   ::v/ex
   {:file "portal/ui/viewer/exception.cljs"
    :examples (map Throwable->map
                   [(::d/exception d/platform-data)
                    (::d/user-exception d/platform-data)
                    (::d/io-exception d/platform-data)
                    (::d/ex-info d/platform-data)])}
   ::v/http
   {:file "portal/ui/viewer/http.cljs"
    :examples (concat d/http-requests d/http-responses)}
   ::v/test-report
   {:file "portal/ui/viewer/test_report.cljs"
    :examples [d/test-report]}
   ::v/vega
   {:file "portal/ui/viewer/vega.cljs"
    :examples (vals (::d/vega d/data-visualization))}
   ::v/vega-lite
   {:file "portal/ui/viewer/vega_lite.cljs"
    :examples (vals (::d/vega-lite d/data-visualization))}
   ::v/bin
   {:examples [(::d/binary d/platform-data)]}
   ::v/image
   {:examples [(::d/binary d/platform-data)]}
   ::v/table
   {:examples [(with-meta d/log-data {::v/default :portal.viewer/table})]}
   ::v/hiccup
   {:examples [d/hiccup]}
   ::v/date-time
   {:examples [(java.util.Date.)]}
   #_#_::v/diff
     {:examples [(vary-meta d/diff-data dissoc ::v/default)]}
   ::v/tree
   {:examples [(vary-meta d/hiccup dissoc ::v/default)]}
   ::v/code
   {:examples ["(+ 1 2 3)"]}
   ::v/pr-str
   {:examples ['(+ 1 2 3)]}
   ::v/json
   {:examples [(::d/json d/string-data)]}
   ::v/edn
   {:examples [(::d/edn d/string-data)]}
   #_#_::v/csv
     {:examples [(::d/csv d/string-data)]}
   ::v/markdown
   {:examples [(::d/markdown d/string-data)]}
   ::v/text
   {:examples [(::d/markdown d/string-data)]}
   ::v/pprint
   {:examples [d/basic-data]}})

(defn ->render [{:keys [doc spec examples] :as entry}]
  [(name (:name entry))
   {:hiccup
    [:div
     [:h1 [:portal.viewer/inspector (:name entry)]]
     (when doc [:p doc])
     (when spec
       [:<>
        [:h2 "Spec"]
        [:pre {} [:code {:class "clojure"} spec]]])
     (when examples
       (into
        [:<>
         [:h2 "Examples"]]
        (map-indexed
         (fn [idx itm] ^{:key idx} [(:name entry) itm])
         examples)))]}])

(defn gen-viewer-docs []
  (for [viewer (p/eval-str "(map #(select-keys % [:name :doc]) @portal.ui.api/viewers)")
        :let [{:keys [file] :as info}  (get info (:name viewer))
              info (cond-> (merge viewer info)
                     file
                     (into
                      (->> (slurp (io/resource file))
                           (re-seq #"(?s);;;([^\n]+)\n(.*);;;")
                           (map
                            (fn [[_ k v]] [(edn/read-string k) v])))))]
        :when (or (:doc info) (:examples info) (:spec info))]
    (->render info)))

(defn gen-docs []
  (walk/postwalk
   (fn [v]
     (let [file (:file v)]
       (cond-> v
         file (assoc :markdown (slurp file))
         (and (vector? v) (= (first v) "Viewers"))
         (concat (gen-viewer-docs)))))
   (edn/read-string (slurp "doc/cljdoc.edn"))))

(comment
  (def docs (atom nil))
  (reset! docs (gen-docs))
  (reset! docs nil)
  (p/open {:mode :dev :window-title "portal-docs" :value docs})
  {:cljdoc.doc/tree (gen-viewer-docs)})
