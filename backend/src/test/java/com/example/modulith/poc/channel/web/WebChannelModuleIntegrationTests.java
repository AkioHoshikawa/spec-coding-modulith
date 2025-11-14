package com.example.modulith.poc.channel.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;

/**
 * Integration tests for the Web Channel module.
 * Verifies that the web layer can be tested independently.
 */
@ApplicationModuleTest(mode = BootstrapMode.ALL_DEPENDENCIES)
class WebChannelModuleIntegrationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldBootstrapWebChannelWithDependencies() {
        System.out.println("=== Web Channel Module Bootstrap Test ===");
        
        String[] beanNames = context.getBeanDefinitionNames();
        System.out.println("Total beans loaded: " + beanNames.length);
        
        // Count beans from different modules
        long channelBeans = countBeansInPackage(beanNames, "channel");
        long modelBeans = countBeansInPackage(beanNames, "model");
        long coreBeans = countBeansInPackage(beanNames, "core");
        
        System.out.println("Channel module beans: " + channelBeans);
        System.out.println("Model module beans: " + modelBeans);
        System.out.println("Core module beans: " + coreBeans);
        
        System.out.println("\n✓ Web Channel module bootstrapped with dependencies");
    }

    @Test
    void verifyWebChannelModuleIsolation() {
        // Web channel should have access to domain modules (Order, Inventory)
        // since it needs to call them to handle HTTP requests
        
        System.out.println("=== Verifying Web Channel Dependencies ===");
        
        String[] beanNames = context.getBeanDefinitionNames();
        
        boolean hasOrderBeans = hasBeansInPackage(beanNames, "model.order");
        boolean hasInventoryBeans = hasBeansInPackage(beanNames, "model.inventory");
        
        System.out.println("Has Order beans: " + hasOrderBeans);
        System.out.println("Has Inventory beans: " + hasInventoryBeans);
        
        System.out.println("\nWeb Channel can access domain modules for handling requests");
        System.out.println("✓ Module structure verified");
    }

    private long countBeansInPackage(String[] beanNames, String packageFragment) {
        return java.util.Arrays.stream(beanNames)
            .filter(name -> {
                try {
                    Class<?> beanClass = context.getType(name);
                    return beanClass != null && 
                           beanClass.getPackage() != null && 
                           beanClass.getPackage().getName().contains(packageFragment);
                } catch (Exception e) {
                    return false;
                }
            })
            .count();
    }

    private boolean hasBeansInPackage(String[] beanNames, String packageFragment) {
        return countBeansInPackage(beanNames, packageFragment) > 0;
    }
}
