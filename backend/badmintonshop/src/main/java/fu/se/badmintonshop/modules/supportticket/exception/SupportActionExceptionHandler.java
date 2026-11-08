package fu.se.badmintonshop.modules.supportticket.exception;

import fu.se.badmintonshop.modules.common.exception.ApiExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes =
                fu.se.badmintonshop.modules.supportticket.controller.SupportActionController.class)
public class SupportActionExceptionHandler extends ApiExceptionHandler {}
