package morph.plugin.views;

import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import morph.annotations.TagLib;
import morph.plugin.views.groovy.GroovyTemplateView;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractCachingViewResolver;
import org.springframework.web.servlet.view.RedirectView;

@Component("morphViewResolver")
public class MorphViewResolver extends AbstractCachingViewResolver implements InitializingBean, Ordered {
	public static final String REDIRECT_URL_PREFIX = "redirect:";

	private SimpleTemplateEngine templateEngine = new SimpleTemplateEngine();
	private List<Object> tagLibs = new ArrayList<Object>();
	private int order = 10;
	
	public MorphViewResolver() {
		this.tagLibs = new ArrayList<Object>();
	}
	
	@Override
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}

	public void registerTagLib(Object tagLib) {
		if (!tagLib.getClass().isAnnotationPresent(TagLib.class)) {
			return;
		}

		Object existing = null;
		TagLib annotation = tagLib.getClass().getAnnotation(TagLib.class);
		
		for (Object current : tagLibs) {
			if (current.getClass().isAnnotationPresent(TagLib.class)) {
				TagLib currentAnnotation = current.getClass().getAnnotation(TagLib.class);
				
				if (currentAnnotation.prefix().equals(annotation.prefix())) {
					existing = current;
				}
			}
		}

		if (existing != null) {
			tagLibs.remove(existing);
		}
		
		tagLibs.add(tagLib);
	}
	
	@Override
	protected View createView(String viewName, Locale locale) throws Exception {
		logger.info("Searching for view " + viewName);
		
		if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
			String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
			return new RedirectView(redirectUrl, true, true);
		}
		
		return super.createView(viewName, locale);
	}
	
	@Override
	public View loadView(String viewName, Locale locale) throws Exception {
		File viewOnDisk = new File(getServletContext().getRealPath("/WEB-INF/views/" + viewName + ".html"));
		
		if (!viewOnDisk.exists()) {
			logger.info("Not found view " + viewName);
			return null;
		}
		
		Template template = templateEngine.createTemplate(viewOnDisk);
	
		return new GroovyTemplateView(template, tagLibs);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ApplicationContext context = getApplicationContext();
		logger.info("Searching for taglibs defined in Spring context");
		
		Map<String, Object> tagLibs = context.getBeansWithAnnotation(TagLib.class);
		
		for (Object tagLib : tagLibs.values()) {
			registerTagLib(tagLib);
		}
	}
}