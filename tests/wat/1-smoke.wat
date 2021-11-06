(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (memory (export "memory") 1)
  (data (i32.const 1024) "a = %d, b = %d\5Cn\00%d + %d = %d\5Cn\00")
  (func $add (export "add") (param $a i32) (param $b i32) (result i32)
    (i64.store (i32.const 0) (i64.extend_i32_s (i32.const 1024)))
    (i64.store (i32.const 8) (i64.extend_i32_s (get_local $a)))
    (i64.store (i32.const 16) (i64.extend_i32_s (get_local $b)))
    (call $printf (i32.const 0) (i32.const 3))
    (return (i32.add (get_local $a) (get_local $b))))
  (func $main (export "main")
    (local $a i32)
    (local $b i32)
    (set_local $a (i32.const 7))
    (set_local $b (i32.const 14))
    (i64.store (i32.const 24) (i64.extend_i32_s (i32.const 1041)))
    (i64.store (i32.const 32) (i64.extend_i32_s (get_local $a)))
    (i64.store (i32.const 40) (i64.extend_i32_s (get_local $b)))
    (i64.store (i32.const 48) (i64.extend_i32_s (call $add (get_local $a) (get_local $b))))
    (call $printf (i32.const 24) (i32.const 4))))
