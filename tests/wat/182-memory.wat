(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $seed (mut i32) (i32.const 57))
  (global $__mm_memory (mut i32) (i32.const 0))
  (global $__mm_stat_freed (mut i32) (i32.const 0))
  (global $__mm_inuse (mut i32) (i32.const 0))
  (global $__mm_avail (mut i32) (i32.const 0))
  (global $__mm_stat_allocated (mut i32) (i32.const 0))
  (global $__mm_extra_offset (mut i32) (i32.const -1))
  (global $@stack (mut i32) (i32.const 1))
  (global $__mm_report_histogram (mut i32) (i32.const 0))
  (global $__mm_min (mut i32) (i32.const 0))
  (memory (export "memory") 1)
  (data (i32.const 1024) "\E2\80\BC\EF\B8\8F ASSERTION: \22size >= 2*sizeof(int)\22 @ line %d\0A\00%6d\00 unlim\00 %d\0A\00 -\0A\00\E2\80\BC\EF\B8\8F ASSERTION: \22integrity == (id ^ 816191)\22 @ line %d\0A\00Starting memory test with %d empty \22unit\22 pointers and %d iterations\0A\00%s\0A\00Finished fixed memory test\0A\00Finished variable memory test\0A\00.\00B<\00>\00S<\00|free=\00")
  (func $mulberry32 (result f64)
    (local $t i32)
    (global.set $seed (i32.add (global.get $seed) (i32.const 1831565813)))
    (set_local $t (global.get $seed))
    (set_local $t (i32.mul (i32.xor (get_local $t) (i32.shr_u (get_local $t) (i32.const 15))) (i32.or (get_local $t) (i32.const 1))))
    (set_local $t (i32.xor (get_local $t) (i32.add (get_local $t) (i32.mul (i32.xor (get_local $t) (i32.shr_u (get_local $t) (i32.const 7))) (i32.or (get_local $t) (i32.const 61))))))
    (f64.div (f64.convert_i32_u (i32.xor (get_local $t) (i32.shr_u (get_local $t) (i32.const 14)))) (f64.const 4.294967296E9)))
  (func $allocate_data (param $id i32) (param $size i32) (result i32)
    (local $@stack_entry i32)
    (local $data i32)
    (set_local $@stack_entry (global.get $@stack))
    (if (i32.lt_s (get_local $size) (i32.const 8))
      (then
        (i64.store (global.get $@stack) (i64.const 38))
        (call $printf (i32.const 1024) (global.get $@stack))
        (unreachable)))
    (set_local $data (call $malloc (get_local $size)))
    (memory.fill (get_local $data) (i32.const 0) (get_local $size))
    (i32.store (get_local $data) (get_local $id))
    (i32.store (i32.add (get_local $data) (i32.const 4)) (i32.xor (get_local $id) (i32.const 816191)))
    (global.set $@stack (get_local $@stack_entry))
    (get_local $data))
  (func $print_histogram
    (local $@stack_entry i32)
    (local $s0 i32)
    (local $lim i32)
    (local $i i32)
    (local $j i32)
    (local $hsize i32)
    (local $histogram i32)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $s0 (i32.const 128))
    (set_local $lim (i32.div_s (get_local $s0) (i32.const 2)))
    (set_local $hsize (global.get $@stack))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 4)))
    (set_local $histogram (call $mm_histogram (get_local $hsize)))
    (block $@block_1_break
      (set_local $j (i32.sub (i32.load (get_local $hsize)) (i32.const 1)))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.eqz (if (result i32) (i32.lt_s (get_local $j) (i32.const 1)) (then (i32.const 0)) (else (i32.ne (i32.eqz (i32.load (i32.add (get_local $histogram) (i32.mul (get_local $j) (i32.const 4))))) (i32.const 0))))))
        (set_local $j (i32.sub (get_local $j) (i32.const 1)))
        (br $@block_1_continue)))
    (block $@block_2_break
      (set_local $i (i32.const 0))
      (loop $@block_2_continue
        (br_if $@block_2_break (i32.gt_s (get_local $i) (get_local $j)))
        (set_local $lim (if (result i32) (i32.gt_s (get_local $i) (i32.const 6)) (then (i32.add (i32.add (i32.mul (i32.add (i32.mul (i32.const 64) (get_local $s0)) (i32.const 12)) (i32.sub (get_local $i) (i32.const 7))) (i32.mul (i32.const 64) (get_local $s0))) (i32.const 8))) (else (i32.mul (i32.const 2) (get_local $lim)))))
        (if (i32.lt_s (get_local $i) (i32.sub (i32.load (get_local $hsize)) (i32.const 1)))
          (then
            (i64.store (global.get $@stack) (i64.extend_i32_s (get_local $lim)))
            (call $printf (i32.const 1077) (global.get $@stack)))
          (else
            (call $printf (i32.const 1081) (global.get $@stack))))
        (if (i32.load (i32.add (get_local $histogram) (i32.mul (get_local $i) (i32.const 4))))
          (then
            (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load (i32.add (get_local $histogram) (i32.mul (get_local $i) (i32.const 4))))))
            (call $printf (i32.const 1088) (global.get $@stack)))
          (else
            (call $printf (i32.const 1093) (global.get $@stack))))
        (set_local $i (i32.add (get_local $i) (i32.const 1)))
        (br $@block_2_continue)))
    (global.set $@stack (get_local $@stack_entry)))
  (func $verify_data (param $data i32)
    (local $@stack_entry i32)
    (local $id i32)
    (local $integrity i32)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $id (i32.load (get_local $data)))
    (set_local $integrity (i32.load (i32.add (get_local $data) (i32.const 4))))
    (if (i32.ne (get_local $integrity) (i32.xor (get_local $id) (i32.const 816191)))
      (then
        (i64.store (global.get $@stack) (i64.const 74))
        (call $printf (i32.const 1097) (global.get $@stack))
        (unreachable)))
    (global.set $@stack (get_local $@stack_entry)))
  (func $test_uniform (param $id i32) (param $n_units i32) (param $n_iter i32) (param $size i32)
    (local $@stack_entry i32)
    (local $storage i32)
    (local $iter i32)
    (local $idx i32)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $storage (call $malloc (i32.mul (get_local $n_units) (i32.const 8))))
    (memory.fill (get_local $storage) (i32.const 0) (i32.mul (get_local $n_units) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (get_local $n_units)))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (get_local $n_iter)))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 8)))
    (call $printf (i32.const 1155) (global.get $@stack))
    (block $@block_1_break
      (set_local $iter (i32.const 0))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $iter) (get_local $n_iter)))
        (set_local $idx (i32.trunc_f64_s (f64.mul (call $mulberry32) (f64.convert_i32_s (get_local $n_units)))))
        (if (i32.load (i32.add (get_local $storage) (i32.mul (get_local $idx) (i32.const 4))))
          (then
            (call $verify_data (i32.load (i32.add (get_local $storage) (i32.mul (get_local $idx) (i32.const 4)))))
            (call $free (i32.load (i32.add (get_local $storage) (i32.mul (get_local $idx) (i32.const 4)))))
            (i32.store (i32.add (get_local $storage) (i32.mul (get_local $idx) (i32.const 4))) (i32.const 0)))
          (else
            (i32.store (i32.add (get_local $storage) (i32.mul (get_local $idx) (i32.const 4))) (call $allocate_data (i32.add (i32.const 1) (get_local $iter)) (get_local $size)))))
        (set_local $iter (i32.add (get_local $iter) (i32.const 1)))
        (br $@block_1_continue)))
    (set_local $iter (call $mm_print_units))
    (i64.store (global.get $@stack) (i64.extend_i32_s (get_local $iter)))
    (call $printf (i32.const 1225) (global.get $@stack))
    (call $free (get_local $iter))
    (call $free (get_local $storage))
    (call $printf (i32.const 1229) (global.get $@stack))
    (global.set $@stack (get_local $@stack_entry)))
  (func $test_nonuniform (param $id i32) (param $n_units i32) (param $n_iter i32) (param $size i32)
    (local $@stack_entry i32)
    (local $storage i32)
    (local $iter i32)
    (local $idx i32)
    (local $r f64)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $storage (call $malloc (i32.mul (get_local $n_units) (i32.const 8))))
    (memory.fill (get_local $storage) (i32.const 0) (i32.mul (get_local $n_units) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (get_local $n_units)))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (get_local $n_iter)))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 8)))
    (call $printf (i32.const 1155) (global.get $@stack))
    (block $@block_1_break
      (set_local $iter (i32.const 0))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $iter) (get_local $n_iter)))
        (set_local $idx (i32.trunc_f64_s (f64.mul (call $mulberry32) (f64.convert_i32_s (get_local $n_units)))))
        (if (i32.load (i32.add (get_local $storage) (i32.mul (get_local $idx) (i32.const 4))))
          (then
            (call $verify_data (i32.load (i32.add (get_local $storage) (i32.mul (get_local $idx) (i32.const 4)))))
            (call $free (i32.load (i32.add (get_local $storage) (i32.mul (get_local $idx) (i32.const 4)))))
            (i32.store (i32.add (get_local $storage) (i32.mul (get_local $idx) (i32.const 4))) (i32.const 0)))
          (else
            (set_local $r (call $mulberry32))
            (i32.store (i32.add (get_local $storage) (i32.mul (get_local $idx) (i32.const 4))) (call $allocate_data (i32.add (i32.const 1) (get_local $iter)) (i32.add (i32.trunc_f64_s (f64.mul (f64.mul (f64.mul (f64.mul (f64.convert_i32_s (i32.sub (get_local $size) (i32.const 8))) (get_local $r)) (get_local $r)) (get_local $r)) (get_local $r))) (i32.const 8))))))
        (set_local $iter (i32.add (get_local $iter) (i32.const 1)))
        (br $@block_1_continue)))
    (set_local $iter (call $mm_print_units))
    (i64.store (global.get $@stack) (i64.extend_i32_s (get_local $iter)))
    (call $printf (i32.const 1225) (global.get $@stack))
    (call $free (get_local $iter))
    (call $free (get_local $storage))
    (call $printf (i32.const 1257) (global.get $@stack))
    (global.set $@stack (get_local $@stack_entry)))
  (func $main (export "main") (result i32)
    (call $test_uniform (i32.const 1) (i32.const 1000) (i32.const 100000) (i32.const 108))
    (call $test_uniform (i32.const 2) (i32.const 10) (i32.const 1000) (i32.const 10000))
    (call $test_nonuniform (i32.const 3) (i32.const 100) (i32.const 10000) (i32.const 100000))
    (call $print_histogram)
    (i32.const 0))
  (func $mm_init (param $mm_min i32)
    (local $i i32)
    (if (global.get $__mm_min)
      (then
        (unreachable)))
    (global.set $__mm_min (get_local $mm_min))
    (global.set $__mm_extra_offset (i32.const 120))
    (global.set $__mm_memory (i32.add (i32.const 1305) (global.get $__mm_extra_offset)))
    (global.set $__mm_avail (i32.const 1305))
    (block $@block_1_break
      (set_local $i (i32.const 0))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.gt_s (get_local $i) (i32.const 6)))
        (i32.store (i32.add (global.get $__mm_avail) (i32.mul (get_local $i) (i32.const 4))) (i32.const -1))
        (set_local $i (i32.add (get_local $i) (i32.const 1)))
        (br $@block_1_continue)))
    (global.set $__mm_report_histogram (i32.add (i32.const 1305) (i32.const 40)))
    (block $@block_2_break
      (set_local $i (i32.const 0))
      (loop $@block_2_continue
        (br_if $@block_2_break (i32.gt_s (get_local $i) (i32.const 6)))
        (i32.store (i32.add (global.get $__mm_report_histogram) (i32.mul (get_local $i) (i32.const 4))) (i32.const 0))
        (set_local $i (i32.add (get_local $i) (i32.const 1)))
        (br $@block_2_continue))))
  (func $malloc (param $size i32) (result i32)
    (local $unit i32)
    (local $n i32)
    (local $idx i32)
    (local $state i32)
    (local $j i32)
    (local $i i32)
    (local $j@block_2 i32)
    (local $@temp_i32 i32)
    (global.set $__mm_stat_allocated (i32.add (global.get $__mm_stat_allocated) (i32.const 1)))
    (if (i32.eqz (global.get $__mm_min))
      (then
        (call $mm_init (i32.const 128))))
    (set_local $unit (i32.add (i32.mul (i32.const 64) (global.get $__mm_min)) (i32.const 12)))
    (if (i32.gt_s (get_local $size) (i32.mul (i32.const 64) (global.get $__mm_min)))
      (then
        (set_local $n (i32.add (i32.const 2) (i32.div_s (i32.sub (get_local $size) (i32.add (i32.mul (i32.const 64) (global.get $__mm_min)) (i32.const 8))) (i32.add (i32.mul (i32.const 64) (global.get $__mm_min)) (i32.const 12)))))
        (set_local $@temp_i32 (i32.add (global.get $__mm_report_histogram) (i32.mul (call $@min_32s (i32.add (get_local $n) (i32.const 6)) (i32.const 19)) (i32.const 4))))
        (i32.store (get_local $@temp_i32) (i32.add (i32.load (get_local $@temp_i32)) (i32.const 1)))
        (set_local $idx (i32.const 0))
        (block $@block_1_1_break
          (loop $@block_1_1_continue
            (br_if $@block_1_1_break (i32.ge_s (get_local $idx) (global.get $__mm_inuse)))
            (set_local $state (i32.load (i32.add (global.get $__mm_memory) (i32.mul (get_local $idx) (get_local $unit)))))
            (if (i32.eqz (get_local $state))
              (then
                (block $@block_1_1_1_1_break
                  (set_local $j (i32.add (get_local $idx) (i32.const 1)))
                  (loop $@block_1_1_1_1_continue
                    (br_if $@block_1_1_1_1_break (i32.eqz (block $@block_1_1_1_1_1_break (result i32) (drop (br_if $@block_1_1_1_1_1_break (i32.const 0) (i32.ge_s (get_local $j) (global.get $__mm_inuse)))) (drop (br_if $@block_1_1_1_1_1_break (i32.const 0) (i32.ge_s (get_local $j) (i32.add (get_local $idx) (get_local $n))))) (drop (br_if $@block_1_1_1_1_1_break (i32.const 0) (i32.load (i32.add (global.get $__mm_memory) (i32.mul (get_local $j) (get_local $unit)))))) (i32.const 1))))
                    (set_local $j (i32.add (get_local $j) (i32.const 1)))
                    (br $@block_1_1_1_1_continue)))
                (if (i32.ge_s (i32.sub (get_local $j) (get_local $idx)) (get_local $n))
                  (then
                    (i32.store (i32.add (global.get $__mm_memory) (i32.mul (get_local $idx) (get_local $unit))) (i32.sub (i32.const 0) (get_local $n)))
                    (return (i32.add (i32.add (global.get $__mm_memory) (i32.mul (get_local $idx) (get_local $unit))) (i32.const 4)))))
                (br_if $@block_1_1_break (i32.eq (get_local $j) (global.get $__mm_inuse)))
                (set_local $idx (get_local $j)))
              (else
                (if (i32.lt_s (get_local $state) (i32.const 0))
                  (then
                    (set_local $idx (i32.add (get_local $idx) (i32.sub (i32.const 0) (get_local $state)))))
                  (else
                    (set_local $idx (i32.add (get_local $idx) (i32.const 1)))))))
            (br $@block_1_1_continue)))
        (if (i32.gt_s (get_local $idx) (global.get $__mm_inuse))
          (then
            (unreachable)))
        (set_local $state (i32.sub (get_local $n) (i32.sub (global.get $__mm_inuse) (get_local $idx))))
        (if (i32.le_s (get_local $state) (i32.const 0))
          (then
            (unreachable)))
        (if (i32.gt_s (get_local $state) (get_local $n))
          (then
            (unreachable)))
        (set_local $j (i32.add (i32.div_s (i32.add (i32.add (i32.const 1305) (global.get $__mm_extra_offset)) (i32.mul (i32.add (global.get $__mm_inuse) (get_local $state)) (get_local $unit))) (i32.const 64000)) (i32.const 1)))
        (if (i32.gt_s (get_local $j) (memory.size))
          (then
            (drop (memory.grow (i32.sub (get_local $j) (memory.size))))))
        (block $@block_1_3_break
          (set_local $i (i32.const 0))
          (loop $@block_1_3_continue
            (br_if $@block_1_3_break (i32.ge_s (get_local $i) (get_local $state)))
            (i32.store (i32.add (global.get $__mm_memory) (i32.mul (i32.add (get_local $i) (global.get $__mm_inuse)) (get_local $unit))) (i32.const 0))
            (set_local $i (i32.add (get_local $i) (i32.const 1)))
            (br $@block_1_3_continue)))
        (global.set $__mm_inuse (i32.add (global.get $__mm_inuse) (get_local $state)))
        (i32.store (i32.add (global.get $__mm_memory) (i32.mul (get_local $idx) (get_local $unit))) (i32.sub (i32.const 0) (get_local $n)))
        (return (i32.add (i32.add (global.get $__mm_memory) (i32.mul (get_local $idx) (get_local $unit))) (i32.const 4))))
      (else
        (set_local $n (global.get $__mm_min))
        (block $@block_2_1_break
          (set_local $idx (i32.const 0))
          (loop $@block_2_1_continue
            (br_if $@block_2_1_break (i32.ge_s (get_local $n) (get_local $size)))
            (if (i32.gt_s (get_local $idx) (i32.const 6))
              (then
                (unreachable)))
            (set_local $idx (i32.add (get_local $idx) (i32.const 1)))
            (set_local $n (i32.mul (get_local $n) (i32.const 2)))
            (br $@block_2_1_continue)))
        (i32.store (i32.add (global.get $__mm_report_histogram) (i32.mul (get_local $idx) (i32.const 4))) (i32.add (i32.load (i32.add (global.get $__mm_report_histogram) (i32.mul (get_local $idx) (i32.const 4)))) (i32.const 1)))
        (set_local $state (i32.load (i32.add (global.get $__mm_avail) (i32.mul (get_local $idx) (i32.const 4)))))
        (if (i32.lt_s (get_local $state) (i32.const 0))
          (then
            (set_local $state (i32.const 0))
            (block $@block_2_2_1_break
              (loop $@block_2_2_1_continue
                (br_if $@block_2_2_1_break (i32.ge_s (get_local $state) (global.get $__mm_inuse)))
                (set_local $j (i32.load (i32.add (global.get $__mm_memory) (i32.mul (get_local $state) (get_local $unit)))))
                (if (if (result i32) (i32.eqz (get_local $j)) (then (i32.const 1)) (else (i32.ne (if (result i32) (i32.ne (get_local $j) (i32.add (get_local $idx) (i32.const 1))) (then (i32.const 0)) (else (i32.ne (i64.ne (i64.load (i32.add (i32.add (global.get $__mm_memory) (i32.mul (get_local $state) (get_local $unit))) (i32.const 4))) (i64.const 0)) (i32.const 0)))) (i32.const 0))))
                  (then
                    (br $@block_2_2_1_break))
                  (else
                    (if (i32.gt_s (get_local $j) (i32.const 0))
                      (then
                        (set_local $state (i32.add (get_local $state) (i32.const 1))))
                      (else
                        (set_local $state (i32.add (get_local $state) (i32.sub (i32.const 0) (get_local $j))))))))
                (br $@block_2_2_1_continue)))
            (if (i32.lt_s (get_local $state) (i32.const 0))
              (then
                (unreachable)))
            (if (i32.gt_s (get_local $state) (global.get $__mm_inuse))
              (then
                (unreachable)))
            (if (i32.eq (get_local $state) (global.get $__mm_inuse))
              (then
                (set_local $j (i32.add (i32.div_s (i32.add (i32.add (i32.const 1305) (global.get $__mm_extra_offset)) (i32.mul (i32.add (global.get $__mm_inuse) (i32.const 1)) (get_local $unit))) (i32.const 64000)) (i32.const 1)))
                (if (i32.gt_s (get_local $j) (memory.size))
                  (then
                    (drop (memory.grow (i32.sub (get_local $j) (memory.size))))))
                (global.set $__mm_inuse (i32.add (global.get $__mm_inuse) (i32.const 1)))
                (i32.store (i32.add (global.get $__mm_memory) (i32.mul (get_local $state) (get_local $unit))) (i32.const 0))))
            (i32.store (i32.add (global.get $__mm_avail) (i32.mul (get_local $idx) (i32.const 4))) (get_local $state))))
        (set_local $j (i32.load (i32.add (global.get $__mm_memory) (i32.mul (get_local $state) (get_local $unit)))))
        (if (i32.eqz (if (result i32) (i32.eqz (get_local $j)) (then (i32.const 1)) (else (i32.ne (i32.eq (get_local $j) (i32.add (get_local $idx) (i32.const 1))) (i32.const 0)))))
          (then
            (unreachable)))
        (if (i32.eqz (get_local $j))
          (then
            (i32.store (i32.add (global.get $__mm_memory) (i32.mul (get_local $state) (get_local $unit))) (i32.add (get_local $idx) (i32.const 1)))
            (set_local $i (i32.shl (i32.const 1) (i32.sub (i32.const 6) (get_local $idx))))
            (i64.store (i32.add (i32.add (global.get $__mm_memory) (i32.mul (get_local $state) (get_local $unit))) (i32.const 4)) (i64.sub (select (i64.const 0) (i64.shl (i64.const 1) (i64.extend_i32_s (get_local $i))) (i32.eq (get_local $i) (i32.const 64))) (i64.const 1)))))
        (set_local $i (i32.add (i32.add (global.get $__mm_memory) (i32.mul (get_local $state) (get_local $unit))) (i32.const 4)))
        (if (i64.eq (i64.load (get_local $i)) (i64.const 0))
          (then
            (unreachable)))
        (set_local $j@block_2 (i32.wrap_i64 (i64.ctz (i64.load (get_local $i)))))
        (i64.store (get_local $i) (i64.xor (i64.load (get_local $i)) (i64.shl (i64.const 1) (i64.extend_i32_s (get_local $j@block_2)))))
        (if (i64.eqz (i64.load (get_local $i)))
          (then
            (i32.store (i32.add (global.get $__mm_avail) (i32.mul (get_local $idx) (i32.const 4))) (i32.const -1))))
        (return (i32.add (i32.add (get_local $i) (i32.const 8)) (i32.mul (get_local $j@block_2) (get_local $n))))))
    (unreachable))
  (func $free (param $address i32)
    (local $unit i32)
    (local $i i32)
    (local $idx i32)
    (local $state i32)
    (local $cur i32)
    (local $n i32)
    (local $a_size i32)
    (local $bits i32)
    (local $j i32)
    (global.set $__mm_stat_freed (i32.add (global.get $__mm_stat_freed) (i32.const 1)))
    (set_local $unit (i32.add (i32.mul (i32.const 64) (global.get $__mm_min)) (i32.const 12)))
    (set_local $idx (i32.div_s (i32.sub (get_local $address) (global.get $__mm_memory)) (get_local $unit)))
    (set_local $state (i32.load (i32.add (global.get $__mm_memory) (i32.mul (get_local $idx) (get_local $unit)))))
    (if (i32.eq (get_local $state) (i32.const 0))
      (then
        (unreachable)))
    (if (i32.lt_s (get_local $state) (i32.const 0))
      (then
        (if (i32.ne (get_local $address) (i32.add (i32.add (global.get $__mm_memory) (i32.mul (get_local $idx) (get_local $unit))) (i32.const 4)))
          (then
            (unreachable)))
        (block $@block_1_1_break
          (set_local $i (i32.const 0))
          (loop $@block_1_1_continue
            (br_if $@block_1_1_break (i32.ge_s (get_local $i) (i32.sub (i32.const 0) (get_local $state))))
            (i32.store (i32.add (global.get $__mm_memory) (i32.mul (i32.add (get_local $idx) (get_local $i)) (get_local $unit))) (i32.const 0))
            (set_local $i (i32.add (get_local $i) (i32.const 1)))
            (br $@block_1_1_continue))))
      (else
        (set_local $cur (i32.add (i32.add (global.get $__mm_memory) (i32.mul (get_local $idx) (get_local $unit))) (i32.const 4)))
        (set_local $n (i32.sub (get_local $state) (i32.const 1)))
        (if (i32.gt_s (get_local $n) (i32.const 6))
          (then
            (unreachable)))
        (set_local $a_size (global.get $__mm_min))
        (block $@block_2_1_break
          (set_local $i (i32.const 0))
          (loop $@block_2_1_continue
            (br_if $@block_2_1_break (i32.ge_s (get_local $i) (get_local $n)))
            (set_local $a_size (i32.mul (get_local $a_size) (i32.const 2)))
            (set_local $i (i32.add (get_local $i) (i32.const 1)))
            (br $@block_2_1_continue)))
        (set_local $bits (i32.shl (i32.const 1) (i32.sub (i32.const 6) (get_local $n))))
        (set_local $j (i32.div_s (i32.sub (i32.sub (get_local $address) (get_local $cur)) (i32.const 8)) (get_local $a_size)))
        (if (i32.eqz (if (result i32) (i32.lt_s (get_local $j) (i32.const 0)) (then (i32.const 0)) (else (i32.ne (i32.lt_s (get_local $j) (get_local $bits)) (i32.const 0)))))
          (then
            (unreachable)))
        (if (i32.ne (get_local $address) (i32.add (i32.add (get_local $cur) (i32.const 8)) (i32.mul (get_local $j) (get_local $a_size))))
          (then
            (unreachable)))
        (if (i32.wrap_i64 (i64.and (i64.load (get_local $cur)) (i64.shl (i64.const 1) (i64.extend_i32_s (get_local $j)))))
          (then
            (unreachable)))
        (i64.store (get_local $cur) (i64.xor (i64.load (get_local $cur)) (i64.shl (i64.const 1) (i64.extend_i32_s (get_local $j)))))
        (if (i32.lt_s (i32.load (i32.add (global.get $__mm_avail) (i32.mul (get_local $n) (i32.const 4)))) (i32.const 0))
          (then
            (i32.store (i32.add (global.get $__mm_avail) (i32.mul (get_local $n) (i32.const 4))) (get_local $idx)))
          (else))
        (if (i32.gt_s (i32.const 1) (i32.wrap_i64 (i64.popcnt (i64.load (get_local $cur)))))
          (then
            (unreachable)))
        (if (i32.gt_s (i32.wrap_i64 (i64.popcnt (i64.load (get_local $cur)))) (get_local $bits))
          (then
            (unreachable)))
        (if (if (result i32) (i32.eq (i32.load (i32.add (global.get $__mm_avail) (i32.mul (get_local $n) (i32.const 4)))) (get_local $idx)) (then (i32.const 0)) (else (i32.ne (i32.eq (i32.wrap_i64 (i64.popcnt (i64.load (get_local $cur)))) (get_local $bits)) (i32.const 0))))
          (then
            (i32.store (i32.add (global.get $__mm_memory) (i32.mul (get_local $idx) (get_local $unit))) (i32.const 0)))))))
  (func $__mm_itoa (param $a i32) (param $ret i32)
    (local $@stack_entry i32)
    (local $N i32)
    (local $buf i32)
    (local $n i32)
    (local $d i32)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $N (i32.const 10))
    (set_local $buf (global.get $@stack))
    (global.set $@stack (i32.add (global.get $@stack) (get_local $N)))
    (set_local $n (get_local $N))
    (loop $@block_1_continue
      (set_local $d (i32.rem_s (get_local $a) (i32.const 10)))
      (set_local $a (i32.div_s (get_local $a) (i32.const 10)))
      (set_local $n (i32.sub (get_local $n) (i32.const 1)))
      (i32.store8 (i32.add (get_local $buf) (get_local $n)) (i32.add (i32.const 48) (get_local $d)))
      (br_if $@block_1_continue (i32.gt_s (get_local $a) (i32.const 0))))
    (memory.copy (get_local $ret) (i32.add (get_local $buf) (get_local $n)) (i32.sub (get_local $N) (get_local $n)))
    (i32.store8 (i32.add (get_local $ret) (i32.sub (get_local $N) (get_local $n))) (i32.const 0))
    (global.set $@stack (get_local $@stack_entry)))
  (func $__mm_append_string (param $dst i32) (param $src i32)
    (local $len_dst i32)
    (local $len_src i32)
    (block $@block_1_break
      (set_local $len_src (i32.const 0))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.eqz (i32.load8_s (i32.add (get_local $src) (get_local $len_src)))))
        (set_local $len_src (i32.add (get_local $len_src) (i32.const 1)))
        (br $@block_1_continue)))
    (block $@block_2_break
      (set_local $len_dst (i32.const 0))
      (loop $@block_2_continue
        (br_if $@block_2_break (i32.eqz (i32.load8_s (i32.add (get_local $dst) (get_local $len_dst)))))
        (set_local $len_dst (i32.add (get_local $len_dst) (i32.const 1)))
        (br $@block_2_continue)))
    (memory.copy (i32.add (get_local $dst) (get_local $len_dst)) (get_local $src) (i32.add (get_local $len_src) (i32.const 1))))
  (func $__mm_append_number (param $dst i32) (param $num i32)
    (local $@stack_entry i32)
    (local $buf i32)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $buf (global.get $@stack))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 10)))
    (call $__mm_itoa (get_local $num) (get_local $buf))
    (call $__mm_append_string (get_local $dst) (get_local $buf))
    (global.set $@stack (get_local $@stack_entry)))
  (func $mm_print_units (result i32)
    (local $unit i32)
    (local $buf i32)
    (local $idx i32)
    (local $state i32)
    (set_local $unit (i32.add (i32.mul (i32.const 64) (global.get $__mm_min)) (i32.const 12)))
    (set_local $buf (call $malloc (i32.const 1000)))
    (i32.store8 (get_local $buf) (i32.const 0))
    (block $@block_1_break
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $idx) (global.get $__mm_inuse)))
        (set_local $state (i32.load (i32.add (global.get $__mm_memory) (i32.mul (get_local $idx) (get_local $unit)))))
        (if (i32.eqz (get_local $state))
          (then
            (call $__mm_append_string (get_local $buf) (i32.const 1288)))
          (else
            (if (i32.lt_s (get_local $state) (i32.const 0))
              (then
                (call $__mm_append_string (get_local $buf) (i32.const 1290))
                (call $__mm_append_number (get_local $buf) (i32.sub (i32.const 0) (get_local $state)))
                (call $__mm_append_string (get_local $buf) (i32.const 1293))
                (set_local $idx (i32.add (get_local $idx) (i32.sub (i32.sub (i32.const 0) (get_local $state)) (i32.const 1)))))
              (else
                (call $__mm_append_string (get_local $buf) (i32.const 1295))
                (call $__mm_append_number (get_local $buf) (i32.sub (get_local $state) (i32.const 1)))
                (call $__mm_append_string (get_local $buf) (i32.const 1298))
                (call $__mm_append_number (get_local $buf) (i32.wrap_i64 (i64.popcnt (i64.load (i32.add (i32.add (global.get $__mm_memory) (i32.mul (get_local $idx) (get_local $unit))) (i32.const 4))))))
                (call $__mm_append_string (get_local $buf) (i32.const 1293))))))
        (set_local $idx (i32.add (get_local $idx) (i32.const 1)))
        (br $@block_1_continue)))
    (get_local $buf))
  (func $mm_histogram (param $p_count i32) (result i32)
    (i32.store (get_local $p_count) (i32.const 20))
    (global.get $__mm_report_histogram))
  (func $@min_32s (param $a i32) (param $b i32) (result i32)
    (select (get_local $b) (get_local $a) (i32.gt_s (get_local $a) (get_local $b)))))
