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
package org.springframework.samples.petclinic.vet;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
class VetController {
    private static final long VETS_ASYNC_SLEEP_MS = Long.getLong("vetsAsyncSleep", 500);
    private static final int VETS_SYNTHETIC_SPAN_NB = Integer.getInteger("vetsSyntheticSpans", 0);
    private static final int VETS_SYNTHETIC_SPAN_SLEEP_MS = Integer.getInteger("vetsSyntheticSpanSleep", 10);

	private final VetRepository vets;

	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "VetsExecutor"));

	public VetController(VetRepository clinicService) {
		this.vets = clinicService;
	}

	@GetMapping("/vets.html")
	public String showVetList(Map<String, Object> model) {
		// Here we are returning an object of type 'Vets' rather than a collection of Vet
		// objects so it is simpler for Object-Xml mapping
		Vets vets = new Vets();
		vets.getVetList().addAll(this.vets.findAll());
		// synthetic spans
        for (int i = 0; i < VETS_SYNTHETIC_SPAN_NB; i++) {
            syntheticSpan();
        }
		// async
		Tracer tracer = GlobalTracer.get();
		Span asyncSpan = tracer.buildSpan("VetsAsync").start();
		CompletableFuture.runAsync(() -> {
		    try (Scope scope = tracer.activateSpan(asyncSpan)) {
                LockSupport.parkNanos(Duration.ofMillis(VETS_ASYNC_SLEEP_MS).toNanos());
            }
		}).whenComplete((u, throwable) -> {
			asyncSpan.finish();
		});
		model.put("vets", vets);
		return "vets/vetList";
	}

	@GetMapping({ "/vets" })
	public @ResponseBody Vets showResourcesVetList() {
		// Here we are returning an object of type 'Vets' rather than a collection of Vet
		// objects so it is simpler for JSon/Object mapping
		Vets vets = new Vets();
		vets.getVetList().addAll(this.vets.findAll());
		return vets;
	}

	private void syntheticSpan() {
        Tracer tracer = GlobalTracer.get();
        Span synSpan = tracer.buildSpan("synthetic").start();
        try (Scope scope = tracer.activateSpan(synSpan)) {
            LockSupport.parkNanos(Duration.ofMillis(VETS_SYNTHETIC_SPAN_SLEEP_MS).toNanos());
        } finally {
            synSpan.finish();
        }
    }

}
