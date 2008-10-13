/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jsecurity.authz.aop;

import org.jsecurity.aop.MethodInvocation;
import org.jsecurity.authz.AuthorizationException;
import org.jsecurity.authz.UnauthenticatedException;
import org.jsecurity.authz.annotation.RequiresUser;

/**
 * Checks to see if a @{@link org.jsecurity.authz.annotation.RequiresUser RequiresUser} annotation
 * is declared, and if so, ensures the calling <code>Subject</code> is <em>either</em>
 * {@link org.jsecurity.subject.Subject#isAuthenticated() authenticated} <b><em>or</em></b> remembered via remember
 * me services before invoking the method.
 * <p>
 * This annotation essentially ensures that <code>subject.{@link org.jsecurity.subject.Subject#getPrincipal() getPrincipal()} != null</code>.
 *
 * @author Les Hazlewood
 * @since 0.9.0 RC3
 */
public class UserAnnotationMethodInterceptor extends AuthorizingAnnotationMethodInterceptor {

    /**
     * Default no-argument constructor that ensures this interceptor looks for
     *
     * @{@link org.jsecurity.authz.annotation.RequiresUser RequiresUser} annotations in a method
     * declaration.
     */
    public UserAnnotationMethodInterceptor() {
        super(RequiresUser.class);
    }

    /**
     * Ensures that the calling <code>Subject</code> is a <em>user</em>, that is, they are <em>either</code>
     * {@link org.jsecurity.subject.Subject#isAuthenticated() authenticated} <b><em>or</em></b> remembered via remember
     * me services before invoking the method, and if not, throws an
     * <code>AuthorizingException</code> indicating the method is not allowed to be executed.
     *
     * @param mi the method invocation to check for one or more roles
     * @throws org.jsecurity.authz.AuthorizationException
     *          if the calling <code>Subject</code> is not authenticated or remembered via rememberMe services.
     */
    public void assertAuthorized(MethodInvocation mi) throws AuthorizationException {
        RequiresUser annotation = (RequiresUser) getAnnotation(mi);
        if (annotation != null) {
            if (getSubject().getPrincipal() == null) {
                throw new UnauthenticatedException("Attempting to access a user-only method.  The current Subject is " +
                        "not a user (they haven't been authenticated or remembered from a previous login).  " +
                        "Method invocation denied.");
            }
        }
    }
}
