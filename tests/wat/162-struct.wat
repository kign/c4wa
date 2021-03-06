(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $@stack (mut i32) (i32.const 8))
  (memory (export "memory") 1)
  (data (i32.const 1024) "Point %s: [%.6f, %.6f, %.6f]\0A\00green\00")
  (func $strlen (param $str i32) (result i32)
    (local $n i32)
    (loop $@block_1_continue
      (set_local $str (i32.add (get_local $str) (i32.const 1)))
      (set_local $n (i32.add (get_local $n) (i32.const 1)))
      (br_if $@block_1_continue (i32.ne (i32.load8_s align=1 (get_local $str)) (i32.const 0))))
    (get_local $n))
  (func $init_point (param $p i32) (param $color i32) (param $x f64) (param $y f64) (param $z f64)
    (memory.copy (get_local $p) (get_local $color) (i32.add (i32.const 1) (call $strlen (get_local $color))))
    (f32.store align=4 (i32.add (get_local $p) (i32.const 12)) (f32.demote_f64 (get_local $x)))
    (f32.store align=4 (i32.add (get_local $p) (i32.const 16)) (f32.demote_f64 (get_local $y)))
    (f32.store align=4 (i32.add (get_local $p) (i32.const 20)) (f32.demote_f64 (get_local $z))))
  (func $print_point (param $p i32)
    (local $@stack_entry i32)
    (set_local $@stack_entry (global.get $@stack))
    (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (get_local $p)))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (f64.store align=8 (global.get $@stack) (f64.promote_f32 (f32.load align=4 (i32.add (get_local $p) (i32.const 12)))))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (f64.store align=8 (global.get $@stack) (f64.promote_f32 (f32.load align=4 (i32.add (get_local $p) (i32.const 16)))))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (f64.store align=8 (global.get $@stack) (f64.promote_f32 (f32.load align=4 (i32.add (get_local $p) (i32.const 20)))))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 24)))
    (call $printf (i32.const 1024) (global.get $@stack))
    (global.set $@stack (get_local $@stack_entry)))
  (func $main (export "main") (result i32)
    (local $@stack_entry i32)
    (local $a i32)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $a (global.get $@stack))
    (global.set $@stack (i32.add (i32.const 8) (i32.mul (i32.const 8) (i32.div_s (i32.sub (i32.add (global.get $@stack) (i32.const 24)) (i32.const 1)) (i32.const 8)))))
    (f32.store align=4 (i32.add (get_local $a) (i32.const 12)) (f32.const 1.0))
    (i32.store8 align=1 (get_local $a) (i32.const 97))
    (i32.store8 align=1 (i32.add (get_local $a) (i32.const 1)) (i32.const 98))
    (call $init_point (get_local $a) (i32.const 1054) (f64.const -3.5) (f64.const 8.6) (f64.const 4.2))
    (call $print_point (get_local $a))
    (global.set $@stack (get_local $@stack_entry))
    (i32.const 0)))
