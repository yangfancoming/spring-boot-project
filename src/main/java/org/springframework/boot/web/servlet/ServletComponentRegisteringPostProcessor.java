

package org.springframework.boot.web.servlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link BeanFactoryPostProcessor} that registers beans for Servlet components found via
 * package scanning.
 * 它的作用就是：扫描被@WebServlet、@WebFilter及@WebListener的类，最后通过对应的ServletRegistrationBean、FilterRegistrationBean及ServletListenerRegistrationBean进行注册
 * @author Andy Wilkinson
 * @see ServletComponentScan
 * @see ServletComponentScanRegistrar
 */
class ServletComponentRegisteringPostProcessor implements BeanFactoryPostProcessor, ApplicationContextAware {

	private static final List<ServletComponentHandler> HANDLERS;

	static {
		List<ServletComponentHandler> servletComponentHandlers = new ArrayList<>();
		servletComponentHandlers.add(new WebServletHandler());
		servletComponentHandlers.add(new WebFilterHandler());
		servletComponentHandlers.add(new WebListenerHandler());
		HANDLERS = Collections.unmodifiableList(servletComponentHandlers);
	}

	private final Set<String> packagesToScan;

	private ApplicationContext applicationContext;

	ServletComponentRegisteringPostProcessor(Set<String> packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (isRunningInEmbeddedWebServer()) {
			ClassPathScanningCandidateComponentProvider componentProvider = createComponentProvider();
			for (String packageToScan : this.packagesToScan) {
				scanPackage(componentProvider, packageToScan);
			}
		}
	}

	// 通过componentProvider.findCandidateComponents(packageToScan)方法获取到对应的注解类，同时判断是否为以上说的三种，最后调用其doHandle方法完成注册功能
	private void scanPackage(ClassPathScanningCandidateComponentProvider componentProvider,String packageToScan) {
		for (BeanDefinition candidate : componentProvider.findCandidateComponents(packageToScan)) {
			if (candidate instanceof ScannedGenericBeanDefinition) {
				for (ServletComponentHandler handler : HANDLERS) {
					handler.handle(((ScannedGenericBeanDefinition) candidate),(BeanDefinitionRegistry) this.applicationContext);
				}
			}
		}
	}

	private boolean isRunningInEmbeddedWebServer() {
		return this.applicationContext instanceof WebApplicationContext
				&& ((WebApplicationContext) this.applicationContext)
						.getServletContext() == null;
	}

	private ClassPathScanningCandidateComponentProvider createComponentProvider() {
		ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
				false);
		componentProvider.setEnvironment(this.applicationContext.getEnvironment());
		componentProvider.setResourceLoader(this.applicationContext);
		for (ServletComponentHandler handler : HANDLERS) {
			componentProvider.addIncludeFilter(handler.getTypeFilter());
		}
		return componentProvider;
	}

	Set<String> getPackagesToScan() {
		return Collections.unmodifiableSet(this.packagesToScan);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

}
