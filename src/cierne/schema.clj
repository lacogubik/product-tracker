(ns cierne.schema
  (:require [schema.core :as s]))

(s/defschema Book
             {:title   s/Str
              :author  (s/maybe s/Str)
              :url     s/Str
              :img-url s/Str})
