(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $@stack (mut i32) (i32.const 8))
  (memory (export "memory") 1)
  (data (i32.const 1024) "Result is %.4f\0A\00fltVal > 1 = %d\0A\00[Testing compile-time <<]\0A\002^63 = %.15e [unsigned]\0A\002^31 = %.2f [signed]\0A\002^31 = %.2f [unsigned]\0A\00[Testing run-time <<]\0A\00")
  (func $no_argument_float (result f32)
    (f32.const -141.18))
  (func $no_argument_short (result i32)
    (i32.const 57))
  (func $add_11 (param $x f32) (result f64)
    (f64.promote_f32 (f32.add (get_local $x) (f32.const 11.0))))
  (func $add_19 (param $x f64) (result f32)
    (f32.add (f32.demote_f64 (get_local $x)) (f32.const 19.0)))
  (func $main (export "main") (result i32)
    (local $@stack_entry i32)
    (local $a i32)
    (local $b i32)
    (local $longVal i64)
    (local $fltVal f32)
    (local $dblVal f64)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $dblVal (f64.promote_f32 (call $no_argument_float)))
    (set_local $longVal (i64.const -18))
    (set_local $fltVal (f32.const 210000.0))
    (set_local $fltVal (f32.convert_i64_s (get_local $longVal)))
    (set_local $longVal (i64.extend_i32_s (call $no_argument_short)))
    (f64.store align=8 (global.get $@stack) (f64.add (f64.add (f64.add (f64.add (f64.add (get_local $dblVal) (f64.convert_i64_s (get_local $longVal))) (call $add_11 (f32.const -4.0))) (call $add_11 (f32.const 3.2))) (f64.promote_f32 (f32.div (get_local $fltVal) (f32.const 2.0)))) (f64.promote_f32 (call $add_19 (f64.promote_f32 (get_local $fltVal))))))
    (call $printf (i32.const 1024) (global.get $@stack))
    (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (f64.gt (f64.promote_f32 (get_local $fltVal)) (f64.const 1.0))))
    (call $printf (i32.const 1040) (global.get $@stack))
    (call $printf (i32.const 1057) (global.get $@stack))
    (set_local $fltVal (f32.const 9.223372036854776E18))
    (f64.store align=8 (global.get $@stack) (f64.promote_f32 (get_local $fltVal)))
    (call $printf (i32.const 1084) (global.get $@stack))
    (set_local $fltVal (f32.const -2.147483648E9))
    (f64.store align=8 (global.get $@stack) (f64.promote_f32 (get_local $fltVal)))
    (call $printf (i32.const 1109) (global.get $@stack))
    (set_local $fltVal (f32.const 2.147483648E9))
    (f64.store align=8 (global.get $@stack) (f64.promote_f32 (get_local $fltVal)))
    (call $printf (i32.const 1131) (global.get $@stack))
    (call $printf (i32.const 1155) (global.get $@stack))
    (set_local $a (i32.const 63))
    (set_local $fltVal (f32.convert_i64_u (i64.shl (i64.const 1) (i64.extend_i32_s (get_local $a)))))
    (f64.store align=8 (global.get $@stack) (f64.promote_f32 (get_local $fltVal)))
    (call $printf (i32.const 1084) (global.get $@stack))
    (set_local $fltVal (f32.const -2.147483648E9))
    (f64.store align=8 (global.get $@stack) (f64.promote_f32 (get_local $fltVal)))
    (call $printf (i32.const 1109) (global.get $@stack))
    (set_local $b (i32.const 31))
    (set_local $fltVal (f32.convert_i32_u (i32.shl (i32.const 1) (get_local $b))))
    (f64.store align=8 (global.get $@stack) (f64.promote_f32 (get_local $fltVal)))
    (call $printf (i32.const 1131) (global.get $@stack))
    (global.set $@stack (get_local $@stack_entry))
    (i32.const 0)))
