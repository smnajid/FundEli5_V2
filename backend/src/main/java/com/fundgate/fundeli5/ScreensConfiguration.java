package com.fundgate.fundeli5;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Dealer app: the Angular routes load the single-page app, so a reload of a screen's URL works. Keep in sync with frontend/src/app/app.routes.ts. */
@Configuration
@Profile("!" + Runtimes.PIPELINE)
class ScreensConfiguration implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/dealer-screen").setViewName("forward:/index.html");
        registry.addViewController("/validation-screen").setViewName("forward:/index.html");
    }
}
