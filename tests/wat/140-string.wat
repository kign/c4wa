(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $@stack (mut i32) (i32.const 0))
  (memory (export "memory") 1)
  (data (i32.const 1024) "Hello!\00String <%s> consists of %d characters: \00'%c'\00, \00.\0A\00")
  (func $strlen (param $str i32) (result i32)
    (local $n i32)
    (loop $@block_1_continue
      (set_local $str (i32.add (get_local $str) (i32.const 1)))
      (set_local $n (i32.add (get_local $n) (i32.const 1)))
      (br_if $@block_1_continue (i32.ne (i32.load8_s (get_local $str)) (i32.const 0))))
    (get_local $n))
  (func $main (export "main") (result i32)
    (local $@stack_entry i32)
    (local $hello i32)
    (local $len i32)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $hello (i32.const 1024))
    (set_local $len (i32.const 57))
    (i64.store (global.get $@stack) (i64.const 1031))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (get_local $hello)))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (call $strlen (get_local $hello))))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 16)))
    (call $printf (global.get $@stack) (i32.const 3))
    (loop $@block_1_continue
      (i64.store (global.get $@stack) (i64.const 1071))
      (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
      (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load8_s (get_local $hello))))
      (global.set $@stack (i32.sub (global.get $@stack) (i32.const 8)))
      (call $printf (global.get $@stack) (i32.const 2))
      (set_local $hello (i32.add (get_local $hello) (i32.const 1)))
      (if (i32.ne (i32.load8_s (get_local $hello)) (i32.const 0))
        (then
          (i64.store (global.get $@stack) (i64.const 1076))
          (global.set $@stack (i32.sub (global.get $@stack) (i32.const 0)))
          (call $printf (global.get $@stack) (i32.const 1))))
      (br_if $@block_1_continue (i32.ne (i32.load8_s (get_local $hello)) (i32.const 0))))
    (i64.store (global.get $@stack) (i64.const 1079))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 0)))
    (call $printf (global.get $@stack) (i32.const 1))
    (global.set $@stack (get_local $@stack_entry))
    (i32.const 0)))
