(ns turtle-graphics.turtles.square.turtle
  "square turtle implementation"
  (:require [complex.number :as n]
            [turtle-graphics.transforms :as t]
            [cljs.core.match :refer-macros [match]]
            [reagent.core :as reagent]
            [cljs.core.async :as async :refer [>! <! put! chan alts! timeout]])
  (:require-macros
   [devcards.core :as dc :refer [defcard deftest defcard-rg defcard-doc]]
   [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(defrecord Square-turtle [position heading])

(def initial-turtle (->Square-turtle n/zero n/one))

(defn app-state-for-turtle
  [turtle]
  (let [position (:position turtle)]
    {:turtle turtle
     :svg {:path [[:M position]]
           :circles []
           :points []}}))

(def initial-app-state
  (app-state-for-turtle initial-turtle))

;; turtle commands
(defrecord Forward [d])
(defrecord Left [])
(defrecord Right [])
(defrecord Circle [color])
(defrecord Point [color])

(defprotocol Command
  (process-command [command app]))

(extend-protocol Command
  Forward
  (process-command [{d :d} app]
    (let [heading (get-in app [:turtle :heading])
          position (get-in app [:turtle :position])
          v (n/mult heading d)
          w (n/add position v)]
      ;; update turtle
      ;; update svg path
      (-> app
          (update-in [:turtle :position] #(n/add % (n/mult heading d)))
          (update-in [:svg :path] #(conj % [:L w])))))
  Left
  (process-command [_ app]
    (update-in app [:turtle :heading] #(n/mult % n/i)))
  Right
  (process-command [_ app]
    (update-in app [:turtle :heading] #(n/mult % n/negative-i)))
  Circle
  (process-command [{color :color} app]
    (let [p (get-in app [:turtle :position])
          h (get-in app [:turtle :heading])
          r (n/length h)
          circle {:stroke :grey :fill color :center p :radius r}]
      (update-in app [:svg :circles] #(conj % circle))))
  Point
  (process-command [{color :color} app]
    (let [p (get-in app [:turtle :position])
          h (get-in app [:turtle :heading])
          r (n/length h)
          circle {:stroke :grey :fill color :center p}]
      (update-in app [:svg :points] #(conj % circle)))))

(comment
  (process-command (->Forward 1) initial-app-state)
  )

(def app-state (reagent/atom initial-app-state))

(defn svg-command->string [command t-fn]
  (match command
         [:M p]
         (let [[px py] (t-fn p)]
           (str "M " px " " py " "))
         [:L p]
         (let [[px py] (t-fn p)]
           (str "L " px " " py " "))
         :else nil))

(defn svg-path-string [svg-commands t-fn]
  (clojure.string/trim
   (clojure.string/join
    (map #(svg-command->string % t-fn) svg-commands))))

(defn svg-circle [circle t-fn]
  (let [{:keys [stroke fill center radius]} circle
        [cx cy] (t-fn center)]
    [:circle {:stroke (t/color-table stroke)
              :fill (t/color-table fill)
              :cx cx
              :cy cy
              :r (t-fn radius)}]))

(defn svg-point [circle t-fn]
  (let [{:keys [stroke fill center radius]} circle
        [cx cy] (t-fn center)]
    [:circle {:stroke (t/color-table stroke)
              :fill (t/color-table fill)
              :cx cx
              :cy cy
              :r 3}]))

(defn render-svg
  "create svg component for svg-commands"
  [app resolution t-fn]
  (let [svg-commands (get-in app [:svg :path])
        circles (get-in app [:svg :circles])
        points(get-in app [:svg :points])
        path-string (svg-path-string svg-commands t-fn)]
    [:svg {:width resolution :height resolution}
     [:path {:d path-string
             :stroke "black" :fill "white"}]
     (into [:g {:className "circle-group"}] (map #(svg-circle % t-fn) circles))
     (into [:g {:className "point-group"}] (map #(svg-point % t-fn) points))]))

(comment
  (let [circle {:stroke :grey :fill :lt-red :center n/zero :radius 1}]
    (svg-circle circle t/t-fn))

  )

(defn render-turtle-component [app-state]
  (let [app @app-state
        t (get-in app [:turtle])
        h (:heading t)
        pos (:position t)
        p (get-in app [:svg :path])
        circles (get-in app [:svg :circles])
        points (get-in app [:svg :points])]
    [:div
     [:p (str ":position" (n/coords pos))]
     [:p (str ":heading" (n/coords h))]
     [:p (str ":svg-path " p)]
     [:p (str ":circles " circles)]
     [:p (str ":points " points)]
     (render-svg app 200 t/t-fn)]))

(def turtle-channel (chan))

(defcard-rg render-turtle
  "
# App-State
"
  [render-turtle-component app-state]
  app-state)

(def t-square
  (flatten (repeat 4 [(->Forward 1) (->Left)])))

(defn two-step-circle [c1 c2]
  (list (->Forward 1)
        (->Circle c1)
        (->Forward -2)
        (->Circle c2)
        (->Forward 1)))

(defn circle-dance [c1 c2 c3 c4]
  (concat (two-step-circle c1 c2)
          (list (->Left))
          (two-step-circle c3 c4)))

(go (loop []
      (let [msg (<! turtle-channel)]
        (println msg)
        (swap! app-state #(process-command msg %))
        (recur))))

(comment
  (go
    (>! turtle-channel :hello))
  (go
    (>! turtle-channel (->Forward 1)))

  (go
    (doseq [c (circle-dance :lt-green :lt-blue :lt-red :lt-purple)]
      (println c)
      (>! turtle-channel c)))
  )