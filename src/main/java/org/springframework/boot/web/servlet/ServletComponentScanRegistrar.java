
package org.springframework.boot.web.servlet;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} used by {@link ServletComponentScan}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class ServletComponentScanRegistrar implements ImportBeanDefinitionRegistrar {

	private static final String BEAN_NAME = "servletComponentRegisteringPostProcessor";

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,BeanDefinitionRegistry registry) {
		Set<String> packagesToScan = getPackagesToScan(importingClassMetadata); // 获取包路径
		if (registry.containsBeanDefinition(BEAN_NAME)) {  // 若已注册，则更新，否则新增
			updatePostProcessor(registry, packagesToScan);
		}else {
			addPostProcessor(registry, packagesToScan);
		}
	}

	private void updatePostProcessor(BeanDefinitionRegistry registry,Set<String> packagesToScan) {
		BeanDefinition definition = registry.getBeanDefinition(BEAN_NAME);
		ValueHolder constructorArguments = definition.getConstructorArgumentValues()	.getGenericArgumentValue(Set.class);
		@SuppressWarnings("unchecked")
		Set<String> mergedPackages = (Set<String>) constructorArguments.getValue();
		mergedPackages.addAll(packagesToScan);
		constructorArguments.setValue(mergedPackages);
	}

	private void addPostProcessor(BeanDefinitionRegistry registry,Set<String> packagesToScan) {

		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(ServletComponentRegisteringPostProcessor.class); // 设置类
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(packagesToScan);// 设置构造函数参数
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(BEAN_NAME, beanDefinition);// 注册
	}

	private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
		// 获取注解ServletComponentScan的属性信息
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(ServletComponentScan.class.getName()));
		// 获取属性basePackages和basePackageClasses
		String[] basePackages = attributes.getStringArray("basePackages");
		Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
		Set<String> packagesToScan = new LinkedHashSet<>(Arrays.asList(basePackages));
		// basePackageClasses 最后也是根据basePackageClasses来获取塔对应的包路径
		for (Class<?> basePackageClass : basePackageClasses) {
			packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
		}
		// 默认不填写时，获取的是被注解类所在包路径，所以一般放在启动类上
		if (packagesToScan.isEmpty()) {
			packagesToScan.add(ClassUtils.getPackageName(metadata.getClassName()));
		}
		return packagesToScan;
	}

}
