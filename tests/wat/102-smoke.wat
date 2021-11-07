(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $N i32 (i32.const 100))
  (memory (export "memory") 1)
  (data (i32.const 1024) "1^2 + 2^2 + ... + %d^2 = %d\5Cn\00")
  (func $main (export "main") (result i32)
    (local $sum i32)
    (local $i i32)
    (set_local $sum (i32.const 0))
    (set_local $i (i32.const 1))
    (loop $@block_1_continue
      (set_local $sum (i32.add (get_local $sum) (i32.mul (get_local $i) (get_local $i))))
      (set_local $i (i32.add (get_local $i) (i32.const 1)))
      (br_if $@block_1_continue (i32.le_s (get_local $i) (global.get $N))))
    (i64.store (i32.const 0) (i64.extend_i32_s (i32.const 1024)))
    (i64.store (i32.const 8) (i64.extend_i32_s (global.get $N)))
    (i64.store (i32.const 16) (i64.extend_i32_s (get_local $sum)))
    (call $printf (i32.const 0) (i32.const 3))
    (return (i32.const 0))))