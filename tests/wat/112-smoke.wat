(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $@stack (mut i32) (i32.const 8))
  (memory (export "memory") 1)
  (data (i32.const 1024) "%d + %d = %d\0A\00")
  (func $add (param $a i32) (param $b i32) (result i32)
    (i32.add (get_local $a) (get_local $b)))
  (func $main (export "main") (result i32)
    (local $@stack_entry i32)
    (local $a i32)
    (local $res i32)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $a (global.get $@stack))
    (global.set $@stack (i32.add (i32.const 8) (i32.mul (i32.const 8) (i32.div_s (i32.sub (i32.add (global.get $@stack) (i32.const 8)) (i32.const 1)) (i32.const 8)))))
    (i32.store align=4 (get_local $a) (i32.const -17))
    (i32.store align=4 (i32.add (get_local $a) (i32.const 4)) (i32.const 11))
    (set_local $res (call $add (i32.load align=4 (get_local $a)) (i32.load align=4 (i32.add (get_local $a) (i32.const 4)))))
    (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (i32.load align=4 (get_local $a))))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (i32.load align=4 (i32.add (get_local $a) (i32.const 4)))))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (get_local $res)))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 16)))
    (call $printf (i32.const 1024) (global.get $@stack))
    (global.set $@stack (get_local $@stack_entry))
    (i32.const 0)))
