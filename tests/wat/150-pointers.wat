(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $@stack (mut i32) (i32.const 0))
  (memory (export "memory") 1)
  (data (i32.const 1024) "%d * %d * %d = %d\0A\00")
  (func $main (export "main") (result i32)
    (local $@stack_entry i32)
    (local $a i32)
    (local $b i32)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $a (i32.const 2058))
    (i32.store (get_local $a) (i32.const 15))
    (set_local $b (i32.add (get_local $a) (i32.const 4)))
    (i32.store (get_local $b) (i32.const 13))
    (i32.store (i32.add (get_local $a) (i32.const 8)) (i32.const 11))
    (i32.store (i32.add (get_local $a) (i32.const 12)) (i32.mul (i32.mul (i32.load (get_local $a)) (i32.load (get_local $b))) (i32.load (i32.add (get_local $a) (i32.const 8)))))
    (i64.store (global.get $@stack) (i64.const 1024))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load (get_local $a))))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load (i32.add (get_local $a) (i32.const 4)))))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load (i32.add (get_local $a) (i32.const 8)))))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load (i32.add (get_local $a) (i32.const 12)))))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 32)))
    (call $printf (global.get $@stack) (i32.const 5))
    (global.set $@stack (get_local $@stack_entry))
    (i32.const 0)))
