(define getM
	(lambda (p)
		(- (expt 2 p) 1)))
		
(define lucasLehmer
	(lambda (p)
		(let ((s 4) (m (getM p)))
				(loop (- p 2) s m))))
		
(define loop 
	(lambda (c s m)
		(if (= c 0)
			(if (= s 0)
				(display (display "Found Mersenne prime: ") (display m))
				(display ((display m) (display " is not a Mersenne prime"))))
			(begin(display "looping\n")
				(display "s is ")
				(display (iterateS s m))
				(display "\n")
				(display "m is ")
				(display m)
				(display "\n")
				(loop (- c 1) (iterateS s m) m)))))

(define iterateS
	(lambda (s m)
		(modulo (- (* s s) 2) m)))
(display (lucasLehmer 17))