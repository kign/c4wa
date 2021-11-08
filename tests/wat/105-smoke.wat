(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $N i32 (i32.const 100))
  (memory (export "memory") 1)
  (data (i32.const 1024) "2\00 %d\00\5Cn\00")
  (func $main (export "main") (result i32)
    (local $p i32)
    (local $found i32)
    (local $d i32)
    (i64.store (i32.const 0) (i64.extend_i32_s (i32.const 1024)))
    (call $printf (i32.const 0) (i32.const 1))
    (block $@block_1_break
      (set_local $p (i32.const 3))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $p) (global.get $N)))
        (set_local $found (i32.const 0))
        (block $@block_1_1_break
          (set_local $d (i32.const 3))
          (loop $@block_1_1_continue
            (br_if $@block_1_1_break (i32.ge_s (i32.mul (get_local $d) (get_local $d)) (get_local $p)))
            (if (i32.eqz (i32.rem_s (get_local $p) (get_local $d)))
              (then
                (set_local $found (i32.const 1))
                (br $@block_1_1_break)))
            (set_local $d (i32.add (get_local $d) (i32.const 2)))
            (br $@block_1_1_continue)))
        (if (i32.eqz (get_local $found))
          (then
            (i64.store (i32.const 8) (i64.extend_i32_s (i32.const 1026)))
            (i64.store (i32.const 16) (i64.extend_i32_s (get_local $p)))
            (call $printf (i32.const 8) (i32.const 2))))
        (set_local $p (i32.add (get_local $p) (i32.const 2)))
        (br $@block_1_continue)))
    (i64.store (i32.const 0) (i64.extend_i32_s (i32.const 1030)))
    (call $printf (i32.const 0) (i32.const 1))
    (return (i32.const 0))))
