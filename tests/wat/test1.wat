(module
  (memory (export "memory") 1)
  (func $add (export "add") (param $a i32) (param $b i32) (result i32)
    (return (i32.add (get_local $a) (get_local $b)))))
