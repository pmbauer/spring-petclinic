/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * PetClinic Spring Boot Application.
 *
 * @author Dave Syer
 *
 */
@SpringBootApplication(proxyBeanMethods = false)
public class PetClinicApplication {

	private static final int NB_THREADS = Integer.getInteger("nbThreads", 0);

	private static ExecutorService executor;

	private static void syntheticWork() {
		syntheticStack(50, () -> {
			LockSupport.parkNanos(TimeUnit.MINUTES.toNanos(30));
		});
	}

	static int syntheticStack(int n, Runnable r) {
		if (n <= 0) {
			r.run();
			return 0;
		}
		return syntheticStack(n - 1, r);
	}

	public static void main(String[] args) {
		if (NB_THREADS > 0) {
			executor = Executors.newFixedThreadPool(NB_THREADS);
			System.out.printf("Thread pools created with threads:%d\n", NB_THREADS);
			for (int i = 0; i < NB_THREADS; i++) {
				executor.submit(PetClinicApplication::syntheticWork);
			}
			System.out.println("Thread created");
		}
		SpringApplication.run(PetClinicApplication.class, args);
	}

}
