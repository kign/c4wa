(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $@stack (mut i32) (i32.const 0))
  (memory (export "memory") 1)
  (data (i32.const 1024) "Hello!\0A\00Comparison: %s\0A\00\22OK\22\00\22FAILED\22\00")
  (func $main (export "main") (result i32)
    (local $@stack_entry i32)
    (local $test i32)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $test (i32.const 1024))
    (i64.store (global.get $@stack) (i64.const 1032))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (select (i32.const 1048) (i32.const 1053) (i32.eq (i32.load8_s (i32.add (get_local $test) (i32.const 6))) (i32.const 10)))))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 8)))
    (call $printf (global.get $@stack) (i32.const 2))
    (global.set $@stack (get_local $@stack_entry))
    (i32.const 0)))
