package com.appsmith.server.controllers;

import com.appsmith.server.constants.Url;
import com.appsmith.server.controllers.ce.CustomJSLibControllerCE;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Url.CUSTOM_JS_LIB_URL)
@Slf4j
public class CustomJSLibController extends CustomJSLibControllerCE {
    // TODO: add args
    public CustomJSLibController() {
        super();
    }
}
