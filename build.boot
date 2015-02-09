(set-env!
 :source-paths    #{"src/cljs" "src/clj"}
 :resource-paths  #{"resources"}
 :dependencies '[[adzerk/boot-cljs      "0.0-2760-0" :scope "test"]
                 [adzerk/boot-cljs-repl "0.2.0"      :scope "test"]
                 [adzerk/boot-reload    "0.2.4"      :scope "test"]
                 [pandeiro/boot-http    "0.6.1"      :scope "test"]
                 [jeluard/boot-notify   "0.1.1"      :scope "test"]
                 [boot-garden           "1.2.5-1"    :scope "test"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [pani "0.0.4-SNAPSHOT" :exclusions [com.firebase/firebase-client-jvm]]
                 [rum "0.2.2"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http    :refer [serve]]
 '[boot-garden.core      :refer [garden]]
 '[jeluard.boot-notify   :refer [notify]])

;; (deftask browser-repl
;;   "Adapted from Mies' script/brepl.clj"
;;   []
;;   (with-pre-wrap fileset
;;     (clojure.main/main "-e" "
;;      (require
;;       '[cljs.repl :as repl]
;;       '[cljs.repl.browser :as browser])

;;      (repl/repl* (browser/repl-env)
;;                  {:output-dir \".browser-repl\"
;;                   :optimizations :none
;;                   :cache-analysis true
;;                   :source-map true})")
;;     fileset))

(deftask build []
  (comp (notify)
        (cljs)
        (garden :styles-var 'chaf-light.styles/screen
                :output-to "css/garden.css")))

(deftask dev-run []
  (comp (serve)
        (watch)
        ;(cljs-repl)
        (reload)
        (build)))

(deftask production []
  (task-options! cljs {:optimizations :advanced
                       :compiler-options {:closure-defines {:goog.DEBUG false}}}
                      garden {:pretty-print false})
  identity)

(deftask development []
  (task-options! cljs {:optimizations :none
                       :unified-mode true
                       :source-map true}
                 reload {:on-jsload 'chaf-light.app/init})
  identity)

(deftask prod
  "Simple alias to run application in production mode
   NO REPL OR AUTOMATIC RELOADING CODE INSERTED"
  []
  (comp (production)
        (serve)
        (watch)
        ;; (cljs-repl)
        (build)))

(deftask dev
  "Simple alias to run application in development mode"
  []
  (comp (development)
        (dev-run)))
