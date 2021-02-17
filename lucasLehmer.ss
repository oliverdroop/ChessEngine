(define remainderZero
	(lambda (n x)
		(if (= (modulo n x) 0) 1 0)))

(define isPrime
	(lambda (n)
		(let ((x 2))
			(recursiveModulo n x))))
			
(define recursiveModulo
	(lambda (n x)
		(if (>= x n)
			1
			(if (= (remainderZero n x) 1)
				0
				(recursiveModulo n (+ x 1))))))
				
(define getM
	(lambda (p)
		(- (expt 2 p) 1)))
		
(define lucasLehmer
	(lambda (p)
		(let ((s 4) (m (getM p)))
			(isMersennePrime (- p 2) s m))))
			
(define isMersennePrime
	(lambda (c s m)
		(if (> c 0)
			(isMersennePrime (- c 1) (iterateS s m) m)
			(if (= s 0) m 0))))
			
(define iterateS
	(lambda (s m)
		(modulo (- (* s s) 2) m)))
		
(define testOrIncrement
	(lambda (x lim)
		(if (= x lim)
			(begin (display "Tested all numbers up to ") (display lim))
			(begin 
				(if (= (isPrime x) 1)
					(let  ((result (lucasLehmer x)))
						(if (= result 0)
							(display "")
							(begin 
								(display "Found Mersenne prime: ") 
								(display result) 
								(display "\n"))))
					(display ""))
				(testOrIncrement (+ x 1) lim)))))
				
(display (testOrIncrement 1 2000))