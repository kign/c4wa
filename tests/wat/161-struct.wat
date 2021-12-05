(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $@stack (mut i32) (i32.const 0))
  (memory (export "memory") 1)
  (data (i32.const 1024) "Calling makeStudent()\0A\00Student name is %s, access = %d\0A\00Vasya\00")
  (func $makeStudent (param $name i32) (param $p_res i32) (result i32)
    (local $@stack_entry i32)
    (local $res i32)
    (set_local $@stack_entry (global.get $@stack))
    (i64.store (global.get $@stack) (i64.const 1024))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 0)))
    (call $printf (global.get $@stack) (i32.const 1))
    (set_local $res (i32.const 2048))
    (i32.store (i32.add (get_local $res) (i32.const 4)) (get_local $name))
    (i32.store (get_local $res) (i32.const 11))
    (i32.store (get_local $p_res) (get_local $res))
    (global.set $@stack (get_local $@stack_entry))
    (get_local $res))
  (func $print_student (param $student i32)
    (local $@stack_entry i32)
    (set_local $@stack_entry (global.get $@stack))
    (i64.store (global.get $@stack) (i64.const 1047))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load (i32.add (get_local $student) (i32.const 4)))))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load (get_local $student))))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 16)))
    (call $printf (global.get $@stack) (i32.const 3))
    (global.set $@stack (get_local $@stack_entry)))
  (func $main (export "main") (result i32)
    (local $p_student i32)
    (local $student i32)
    (local $@temp_i32 i32)
    (set_local $p_student (i32.const 2058))
    (set_local $@temp_i32 (call $makeStudent (i32.const 1080) (get_local $p_student)))
    (i32.store (get_local $@temp_i32) (i32.mul (i32.load (get_local $@temp_i32)) (i32.const 2)))
    (set_local $student (i32.load (get_local $p_student)))
    (call $print_student (get_local $student))
    (i32.store (get_local $student) (i32.sub (i32.load (get_local $student)) (i32.const 1)))
    (call $print_student (i32.load (get_local $p_student)))
    (i32.const 0)))
