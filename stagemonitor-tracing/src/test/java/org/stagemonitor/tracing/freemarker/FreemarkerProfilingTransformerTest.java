package org.stagemonitor.tracing.freemarker;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.SharedMetricRegistries;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.tracing.profiler.CallStackElement;
import org.stagemonitor.tracing.profiler.Profiler;

public class FreemarkerProfilingTransformerTest {
    
    @BeforeClass
    public static void attachProfiler() {
        Stagemonitor.init();
    }

    @Before
    @After
    public void clearMetricRegistry() {
        Stagemonitor.getMetric2Registry().removeMatching(MetricFilter.ALL);
    }

    @AfterClass
    public static void resetStagemonitor() {
        Stagemonitor.reset();
        SharedMetricRegistries.clear();
    }    
    
	@Test
	public void testFreemarkerProfiling() throws Exception {
		final CallStackElement callTree = Profiler.activateProfiling("testFreemarkerProfiling");
		final String renderedTemplate = processTemplate("test.ftl", "${templateModel.foo}", new TemplateModel());
		Profiler.stop();
		Profiler.deactivateProfiling();
		assertThat(renderedTemplate).isEqualTo("foo");
		System.out.println(callTree);

		assertThat(callTree.getChildren()).hasSize(1);
		final CallStackElement freemarkerNode = callTree.getChildren().get(0);
		assertThat(freemarkerNode.getSignature()).isEqualTo("test.ftl:1#templateModel.foo");

		assertThat(freemarkerNode.getChildren()).hasSize(1);
		final CallStackElement templateModelNode = freemarkerNode.getChildren().get(0);
		assertThat(templateModelNode.getSignature()).isEqualTo("String org.stagemonitor.tracing.freemarker.FreemarkerProfilingTransformerTest$TemplateModel.getFoo()");
	}

	@Test
	public void testFreemarkerProfilingMethodCall() throws Exception {
		final CallStackElement callTree = Profiler.activateProfiling("testFreemarkerProfilingMethodCall");
		final String renderedTemplate = processTemplate("test.ftl", "${templateModel.getFoo()}", new TemplateModel());
		Profiler.stop();
		Profiler.deactivateProfiling();
		assertThat(renderedTemplate).isEqualTo("foo");
		System.out.println(callTree);

		assertThat(callTree.getChildren()).hasSize(1);
		final CallStackElement freemarkerNode = callTree.getChildren().get(0);
		assertThat(freemarkerNode.getSignature()).isEqualTo("test.ftl:1#templateModel.getFoo()");

		assertThat(freemarkerNode.getChildren()).hasSize(1);
		final CallStackElement templateModelNode = freemarkerNode.getChildren().get(0);
		assertThat(templateModelNode.getSignature()).isEqualTo("String org.stagemonitor.tracing.freemarker.FreemarkerProfilingTransformerTest$TemplateModel.getFoo()");
	}

	@Test
	public void testFreemarkerWorksIfNotProfiling() throws Exception {
		final String renderedTemplate = processTemplate("test.ftl", "${templateModel.getFoo()}", new TemplateModel());
		assertThat(renderedTemplate).isEqualTo("foo");
	}

	@Test
	public void testShortSignature() {
		final String signature = "foobar.ftl:123#foo.getBar('123').baz";
		// don't try to shorten ftl signatures
		assertThat(CallStackElement.createRoot(signature).getShortSignature()).isNull();
	}

	public static class TemplateModel {
		public String getFoo() {
			Profiler.start("String org.stagemonitor.tracing.freemarker.FreemarkerProfilingTransformerTest$TemplateModel.getFoo()");
			try {
				return "foo";
			} finally {
				Profiler.stop();
			}
		}
	}

	private String processTemplate(String templateName, String templateString, TemplateModel templateModel) throws IOException, TemplateException {
		Template template = new Template(templateName, templateString, new Configuration(Configuration.VERSION_2_3_22));
		StringWriter out = new StringWriter(templateString.length());
		template.process(Collections.singletonMap("templateModel", templateModel), out);
		return out.toString();
	}

}
