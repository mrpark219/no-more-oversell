package me.park.nomoreoversell.common.web;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class UserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String USER_ID_HEADER = "userId";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(UserId.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) throws Exception {
        var userId = webRequest.getHeader(USER_ID_HEADER);
        if (!StringUtils.hasText(userId)) {
            throw new MissingRequestHeaderException(USER_ID_HEADER, parameter);
        }

        try {
            var parsedUserId = Long.valueOf(userId);
            if (parsedUserId <= 0) {
                throw new MethodArgumentTypeMismatchException(
                        userId,
                        Long.class,
                        USER_ID_HEADER,
                        parameter,
                        new IllegalArgumentException("userId must be positive")
                );
            }
            return parsedUserId;
        } catch (NumberFormatException exception) {
            throw new MethodArgumentTypeMismatchException(
                    userId,
                    Long.class,
                    USER_ID_HEADER,
                    parameter,
                    exception
            );
        }
    }
}
