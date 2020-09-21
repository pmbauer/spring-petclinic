package org.springframework.samples.petclinic;

import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

@Component
public class SyntheticStackFilter implements Filter {
    public static final int NB_EXTRA_STACK = Integer.getInteger("nbExtraStack", 0);


    @Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		final IOException[] ioe = { null };
		final ServletException[] se = { null };

		PetClinicApplication.syntheticStack(NB_EXTRA_STACK, () -> {
			try {
				chain.doFilter(request, response);
			}
			catch (IOException e) {
				ioe[0] = e;
			}
			catch (ServletException e) {
				se[0] = e;
			}
		});

		if (ioe[0] != null) {
			throw ioe[0];
		}
		if (se[0] != null) {
			throw se[0];
		}
	}

}
