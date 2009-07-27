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
package org.apache.shiro.mgt;

import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.crypto.Cipher;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.mgt.DelegatingSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * The Shiro framework's default concrete implementation of the {@link SecurityManager} interface,
 * based around a collection of {@link org.apache.shiro.realm.Realm}s.  This implementation delegates its
 * authentication, authorization, and session operations to wrapped {@link Authenticator}, {@link Authorizer}, and
 * {@link org.apache.shiro.session.mgt.SessionManager SessionManager} instances respectively via superclass
 * implementation.
 * <p/>
 * To greatly reduce and simplify configuration, this implementation (and its superclasses) will
 * create suitable defaults for all of its required dependencies, <em>except</em> the required one or more
 * {@link Realm Realm}s.  Because {@code Realm} implementations usually interact with an application's data model,
 * they are almost always application specific;  you will want to specify at least one custom
 * {@code Realm} implementation that 'knows' about your application's data/security model
 * (via {@link #setRealm} or one of the overloaded constructors).  All other attributes in this class hierarchy
 * will have suitable defaults for most enterprise applications.
 * <p/>
 * <b>RememberMe notice</b>: This class supports the ability to configure a
 * {@link #setRememberMeManager RememberMeManager}
 * for {@code RememberMe} identity services for login/logout, BUT, a default instance <em>will not</em> be created
 * for this attribute at startup.
 * <p/>
 * Because RememberMe services are inherently client tier-specific and
 * therefore aplication-dependent, if you want {@code RememberMe} services enabled, you will have to specify an
 * instance yourself via the {@link #setRememberMeManager(RememberMeManager) setRememberMeManager}
 * mutator.  However if you're reading this JavaDoc with the
 * expectation of operating in a Web environment, take a look at the
 * {@code org.apache.shiro.web.DefaultWebSecurityManager} implementation, which
 * <em>does</em> support {@code RememberMe} services by default at startup.
 *
 * @author Les Hazlewood
 * @author Jeremy Haile
 * @since 0.2
 */
public class DefaultSecurityManager extends SessionsSecurityManager {

    //TODO - complete JavaDoc

    private static final Logger log = LoggerFactory.getLogger(DefaultSecurityManager.class);

    protected RememberMeManager rememberMeManager;

    protected SubjectFactory subjectFactory;

    protected SubjectBinder subjectBinder;

    /**
     * Default no-arg constructor.
     */
    public DefaultSecurityManager() {
        super();
        this.subjectFactory = new DefaultSubjectFactory(this);
        this.subjectBinder = new SessionSubjectBinder();
    }

    /**
     * Supporting constructor for a single-realm application.
     *
     * @param singleRealm the single realm used by this SecurityManager.
     */
    public DefaultSecurityManager(Realm singleRealm) {
        this();
        setRealm(singleRealm);
    }

    /**
     * Supporting constructor for multiple {@link #setRealms realms}.
     *
     * @param realms the realm instances backing this SecurityManager.
     */
    public DefaultSecurityManager(Collection<Realm> realms) {
        this();
        setRealms(realms);
    }

    public SubjectFactory getSubjectFactory() {
        return subjectFactory;
    }

    public void setSubjectFactory(SubjectFactory subjectFactory) {
        this.subjectFactory = subjectFactory;
        if (this.subjectFactory instanceof SecurityManagerAware) {
            ((SecurityManagerAware) this.subjectFactory).setSecurityManager(this);
        }
    }

    public SubjectBinder getSubjectBinder() {
        return subjectBinder;
    }

    public void setSubjectBinder(SubjectBinder subjectBinder) {
        this.subjectBinder = subjectBinder;
    }

    public RememberMeManager getRememberMeManager() {
        return rememberMeManager;
    }

    public void setRememberMeManager(RememberMeManager rememberMeManager) {
        this.rememberMeManager = rememberMeManager;
    }

    private AbstractRememberMeManager getRememberMeManagerForCipherAttributes() {
        if (!(this.rememberMeManager instanceof AbstractRememberMeManager)) {
            String msg = "The convenience passthrough methods for setting remember me cipher attributes " +
                    "are only available when the underlying RememberMeManager implementation is a subclass of " +
                    AbstractRememberMeManager.class.getName() + ".";
            throw new IllegalStateException(msg);
        }
        return (AbstractRememberMeManager) this.rememberMeManager;
    }

    public void setRememberMeCipher(Cipher cipher) {
        getRememberMeManagerForCipherAttributes().setCipher(cipher);
    }

    public void setRememberMeCipherKey(byte[] bytes) {
        getRememberMeManagerForCipherAttributes().setCipherKey(bytes);
    }

    public void setRememberMeCipherKeyHex(String hex) {
        getRememberMeManagerForCipherAttributes().setCipherKeyHex(hex);
    }

    public void setRememberMeCipherKeyBase64(String base64) {
        getRememberMeManagerForCipherAttributes().setCipherKeyBase64(base64);
    }

    public void setRememberMeEncryptionCipherKey(byte[] bytes) {
        getRememberMeManagerForCipherAttributes().setEncryptionCipherKey(bytes);
    }

    public void setRememberMeEncryptionCipherKeyHex(String hex) {
        getRememberMeManagerForCipherAttributes().setEncryptionCipherKeyHex(hex);
    }

    public void setRememberMeEncryptionCipherKeyBase64(String base64) {
        getRememberMeManagerForCipherAttributes().setEncryptionCipherKeyBase64(base64);
    }

    public void setRememberMeDecryptionCipherKey(byte[] bytes) {
        getRememberMeManagerForCipherAttributes().setDecryptionCipherKey(bytes);
    }

    public void setRememberMeDecryptionCipherKeyHex(String hex) {
        getRememberMeManagerForCipherAttributes().setDecryptionCipherKeyHex(hex);
    }

    public void setRememberMeDecryptionCipherKeyBase64(String base64) {
        getRememberMeManagerForCipherAttributes().setDecryptionCipherKeyBase64(base64);
    }

    protected Serializable getCurrentSessionId() {
        return ThreadContext.getSessionId();
    }

    protected Session getSession(Serializable id) {
        checkValid(id);
        return new DelegatingSession(this, id);
    }

    protected Session getCurrentSession() {
        Serializable sessionId = getCurrentSessionId();
        Session session = null;
        if (sessionId != null) {
            try {
                session = getSession(sessionId);
            } catch (InvalidSessionException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Session id referenced on the current thread [" + sessionId + "] is invalid.  " +
                            "Ignoring and creating a new Subject instance to continue.  This message can be " +
                            "safely ignored.", e);
                }
            } catch (AuthorizationException e) {
                if (log.isWarnEnabled()) {
                    log.warn("Session id referenced on the current thread [" + sessionId + "] is not allowed to be " +
                            "referenced.  Ignoring and creating a Subject instance without a session to continue.", e);
                }
            }
        }
        return session;
    }

    protected Subject createSubject() {
        Session session = getCurrentSession();
        PrincipalCollection remembered = null;
        //only obtain a remembered identity if the session does not have one:
        if (session != null) {
            if (session.getAttribute(SessionSubjectBinder.PRINCIPALS_SESSION_KEY) == null) {
                remembered = getRememberedIdentity();
            }
        }
        return createSubject(remembered, session);
    }

    /**
     * Returns a {@link Subject} instance that reflects the specified identity (principals), backed by the given
     * {@link Session} instance.  Either argument can be null.
     * <p/>
     * This method is a convenience that assembles either argument into a context {@link Map Map} (if they are
     * not null) and returns {@link #getSubject(java.util.Map)} using the Map as the parameter.
     *
     * @param principals the identity that the constructed {@code Subject} instance should have.
     * @param session    the session to be associated with the constructed {@code Subject} instance.
     * @return The Subject instance reflecting the specified identity (principals) and session.
     * @since 1.0
     */
    protected Subject createSubject(PrincipalCollection principals, Session session) {
        Map<String, Object> context = new HashMap<String, Object>(2);
        if (principals != null && !principals.isEmpty()) {
            context.put(SubjectFactory.PRINCIPALS, principals);
        }
        if (session != null) {
            context.put(SubjectFactory.SESSION, session);
        }
        return getSubject(context);
    }

    /**
     * Creates a {@code Subject} instance for the user represented by the given method arguments.
     *
     * @param token the {@code AuthenticationToken} submitted for the successful authentication.
     * @param info  the {@code AuthenticationInfo} of a newly authenticated user.
     * @return the {@code Subject} instance that represents the user and session data for the newly
     *         authenticated user.
     */
    protected Subject createSubject(AuthenticationToken token, AuthenticationInfo info) {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(SubjectFactory.AUTHENTICATED, Boolean.TRUE);
        context.put(SubjectFactory.AUTHENTICATION_TOKEN, token);
        context.put(SubjectFactory.AUTHENTICATION_INFO, info);
        Subject subject = getSubject(false);
        if (subject != null) {
            context.put(SubjectFactory.SUBJECT, subject);
        }
        return getSubject(context);
    }

    /**
     * Binds a {@code Subject} instance created after authentication to the application for later use.
     * <p/>
     * The default implementation merely binds the argument to the thread local via the {@link ThreadContext}
     * and overridden by subclasses for environment-specific binding (e.g. standalone application).
     *
     * @param subject the {@code Subject} instance created after authentication to be bound to the application
     *                for later use.
     */
    protected void bind(Subject subject) {
        getSubjectBinder().bind(subject);
    }

    protected void rememberMeSuccessfulLogin(AuthenticationToken token, AuthenticationInfo info) {
        RememberMeManager rmm = getRememberMeManager();
        if (rmm != null) {
            try {
                rmm.onSuccessfulLogin(token, info);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    String msg = "Delegate RememberMeManager instance of type [" + rmm.getClass().getName() +
                            "] threw an exception during onSuccessfulLogin.  RememberMe services will not be " +
                            "performed for account [" + info + "].";
                    log.warn(msg, e);
                }
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("This " + getClass().getName() + " instance does not have a " +
                        "[" + RememberMeManager.class.getName() + "] instance configured.  RememberMe services " +
                        "will not be performed for account [" + info + "].");
            }
        }
    }

    protected void rememberMeFailedLogin(AuthenticationToken token, AuthenticationException ex) {
        RememberMeManager rmm = getRememberMeManager();
        if (rmm != null) {
            try {
                rmm.onFailedLogin(token, ex);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    String msg = "Delegate RememberMeManager instance of type [" + rmm.getClass().getName() +
                            "] threw an exception during onFailedLogin for AuthenticationToken [" +
                            token + "].";
                    log.warn(msg, e);
                }
            }
        }
    }

    protected void rememberMeLogout(PrincipalCollection subjectPrincipals) {
        RememberMeManager rmm = getRememberMeManager();
        if (rmm != null) {
            try {
                rmm.onLogout(subjectPrincipals);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    String msg = "Delegate RememberMeManager instance of type [" + rmm.getClass().getName() +
                            "] threw an exception during onLogout for subject with principals [" +
                            subjectPrincipals + "]";
                    log.warn(msg, e);
                }
            }
        }
    }

    /**
     * First authenticates the {@code AuthenticationToken} argument, and if successful, constructs a
     * {@code Subject} instance representing the authenticated account's identity.
     * <p/>
     * Once constructed, the {@code Subject} instance is then {@link #bind bound} to the application for
     * subsequent access before being returned to the caller.
     *
     * @param token the authenticationToken to process for the login attempt.
     * @return a Subject representing the authenticated user.
     * @throws AuthenticationException if there is a problem authenticating the specified {@code token}.
     */
    public Subject login(AuthenticationToken token) throws AuthenticationException {
        AuthenticationInfo info;
        try {
            info = authenticate(token);
            onSuccessfulLogin(token, info);
        } catch (AuthenticationException ae) {
            try {
                onFailedLogin(token, ae);
            } catch (Exception e) {
                if (log.isInfoEnabled()) {
                    log.info("onFailedLogin(AuthenticationToken,AuthenticationException) method threw an " +
                            "exception.  Logging and propagating original AuthenticationException.", e);
                }
            }
            throw ae; //propagate
        }
        Subject subject = createSubject(token, info);
        bind(subject);
        return subject;
    }

    protected void onSuccessfulLogin(AuthenticationToken token, AuthenticationInfo info) {
        rememberMeSuccessfulLogin(token, info);
    }

    protected void onFailedLogin(AuthenticationToken token, AuthenticationException ae) {
        rememberMeFailedLogin(token, ae);
    }

    protected void beforeLogout(PrincipalCollection subjectIdentifier) {
        rememberMeLogout(subjectIdentifier);
    }

    /**
     * This implementation attempts to resolve any session ID that may exist in the context argument by first
     * passing it to the {@link #resolveSessionIfNecessary(java.util.Map) resolveSessionIfNecessary} method.  The
     * return value from that call is then used to create the Subject instance by calling
     * <code>{@link #getSubjectFactory() getSubjectFactory()}.{@link SubjectFactory#createSubject(java.util.Map) createSubject}(returnValue);</code>
     *
     * @param context any data needed to direct how the Subject should be constructed.
     * @return the {@code Subject} instance reflecting the specified initialization data.
     * @see SubjectFactory#createSubject(java.util.Map)
     * @since 1.0
     */
    public Subject getSubject(Map context) {
        //Translate a session id if it exists into a Session object before sending to the SubjectFactory
        //The SubjectFactory should not need to know how to acquire sessions as it is often environment
        //specific - better to shield the SF from these details:
        Map resolved = resolveSessionIfNecessary(context);
        return getSubjectFactory().createSubject(resolved);
    }

    /**
     * Attempts to resolve any session id in the context to its corresponding {@link Session} and returns a
     * context that represents this resolved {@code Session} to ensure it may be referenced if necessary by the
     * invoked {@link SubjectFactory} that performs actual {@link Subject} construction.
     * <p/>
     * The session id, if it exists in the context map, should be available as a value under the
     * <code>{@link SubjectFactory SubjectFactory}.{@link SubjectFactory#SESSION_ID SESSION_ID}</code> key constant.
     * If a session is resolved, a copy of the original context Map is made to ensure the method argument is not
     * changed, the resolved session is placed into the copy and the copy is returned.
     * <p/>
     * If there is a {@code Session} already in the context because that is what the caller wants to be used for
     * {@code Subject} construction, or if no session is resolved, this method effectively does nothing and immediately
     * returns the Map method argument without change.
     *
     * @param context the subject context data that may contain a session id that should be converted to a Session instance.
     * @return The context Map to use to pass to a {@link SubjectFactory} for subject creation.
     * @since 1.0
     */
    @SuppressWarnings({"unchecked"})
    protected Map resolveSessionIfNecessary(Map context) {
        if (context.containsKey(SubjectFactory.SESSION)) {
            log.debug("Context already contains a session.  Returning.");
            return context;
        }
        log.trace("No session found in context.  Looking for a session id to resolve in to a session.");
        //otherwise try to resolve a session if a session id exists:
        Map copy = new HashMap(context);
        Serializable sessionId = getSessionId(context);
        if (sessionId != null) {
            try {
                Session session = getSession(sessionId);
                copy.put(SubjectFactory.SESSION, session);
            } catch (InvalidSessionException e) {
                onInvalidSessionId(sessionId);
                log.debug("Context referenced sessionId is invalid.  Ignoring and creating an anonymous " +
                        "(session-less) Subject instance.", e);
            }
        }
        return copy;
    }

    /**
     * Allows subclasses to react to the fact that a provided session id was invalid.
     *
     * @param sessionId the session id that was discovered to be invalid (no session, expired, etc).
     * @since 1.0
     */
    protected void onInvalidSessionId(Serializable sessionId) {
    }

    /**
     * Utility method to retrieve the session id from the given subject context Map which will be used to resolve
     * to a {@link Session} or {@code null} if there is no session id in the map.  If the session id exists, it is
     * expected to be available in the map under the
     * <code>{@link SubjectFactory SubjectFactory}.{@link SubjectFactory#SESSION_ID SESSION_ID}</code> constant.
     *
     * @param subjectContext the context map with data that will be used to construct a {@link Subject} instance via
     *                       a {@link SubjectFactory}
     * @return a session id to resolve to a {@link Session} instance or {@code null} if a session id could not be found.
     * @see #getSubject(java.util.Map)
     * @see SubjectFactory#createSubject(java.util.Map)
     */
    protected Serializable getSessionId(Map subjectContext) {
        return (Serializable) subjectContext.get(SubjectFactory.SESSION_ID);
    }

    public void logout(Subject subject) {

        if (subject == null) {
            throw new IllegalArgumentException("Subject method argument cannot be null.");
        }

        PrincipalCollection principals = subject.getPrincipals();

        if (principals != null && !principals.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Logging out subject with primary id {}" + principals.iterator().next());
            }
            beforeLogout(principals);
            Authenticator authc = getAuthenticator();
            if (authc instanceof LogoutAware) {
                ((LogoutAware) authc).onLogout(principals);
            }
        }

        try {
            unbind(subject);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                String msg = "Unable to cleanly unbind Subject.  Ignoring (logging out).";
                log.debug(msg, e);
            }
        } finally {
            try {
                stopSession(subject);
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    String msg = "Unable to cleanly stop Session for Subject [" + subject.getPrincipal() + "] " +
                            "Ignoring (logging out).";
                    log.debug(msg, e);
                }
            }
        }
    }

    protected void stopSession(Subject subject) {
        Session s = subject.getSession(false);
        if (s != null) {
            //react to the id and not the session itself - the Session instance could be a proxy/delegate Session
            //in which case the ID might be the only thing accessible.  Better to pass off the ID to the underlying
            //SessionManager since this will successfully handle all cases.
            Serializable sessionId = s.getId();
            if (sessionId != null) {
                try {
                    stop(sessionId);
                } catch (SessionException e) {
                    //ignored - we're invalidating, and have no further need of the session anyway
                    //log just in case someone wants to know:
                    if (log.isDebugEnabled()) {
                        String msg = "Session for Subject [" + (subject != null ? subject.getPrincipal() : null) +
                                "] has already been invalidated.  Logging exception since session exceptions are " +
                                "irrelevant when the owning Subject has logged out.";
                        log.debug(msg, e);
                    }
                }
            }
        }
    }

    protected void unbind(Subject subject) {
        getSubjectBinder().unbind(subject);
    }

    protected PrincipalCollection getRememberedIdentity() {
        RememberMeManager rmm = getRememberMeManager();
        if (rmm != null) {
            try {
                return rmm.getRememberedPrincipals();
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    String msg = "Delegate RememberMeManager instance of type [" + rmm.getClass().getName() +
                            "] threw an exception during getRememberedPrincipals().";
                    log.warn(msg, e);
                }
            }
        }
        return null;
    }

    protected Subject getSubject(boolean create) {
        Subject subject = getSubjectBinder().getSubject();
        if (subject == null && create) {
            subject = createSubject();
            bind(subject);
        }
        return subject;
    }

    public Subject getSubject() {
        return getSubject(true);
    }

    /**
     * Acquires the {@link Subject Subject} that owns the {@link Session Session} with the specified {@code sessionId}.
     * <p/>
     * <b>Although simple in concept, this method provides incredibly powerful functionality:</b>
     * <p/>
     * The ability to reference a {@code Subject} and their server-side session
     * <em>across clients of different mediums</em> such as web applications, Java applets,
     * standalone C# clients over XMLRPC and/or SOAP, and many others. This is a <em>huge</em>
     * benefit in heterogeneous enterprise applications.
     * <p/>
     * To maintain session integrity across client mediums, the {@code sessionId} <b>must</b> be transmitted
     * to all client mediums securely (e.g. over SSL) to prevent man-in-the-middle attacks.  This
     * is nothing new - all web applications are susceptible to the same problem when transmitting
     * {@code Cookie}s or when using URL rewriting.  As long as the
     * {@code sessionId} is transmitted securely, session integrity can be maintained.
     *
     * @param sessionId the id of the session that backs the desired Subject being acquired.
     * @return the {@code Subject} that owns the {@code Session Session} with the specified {@code sessionId}
     * @throws InvalidSessionException if the session identified by {@code sessionId} has been stopped, expired, or
     *                                 doesn't exist.
     * @throws AuthorizationException  if the executor of this method is not allowed to acquire the owning
     *                                 {@code Subject}.  The reason for the exception is implementation-specific and
     *                                 could be for any number of reasons.  A common reason in many systems would be
     *                                 if one host tried to acquire a {@code Subject} based on a {@code Session} that
     *                                 originated on an entirely different host (although it is not a Shiro requirement
     *                                 this scenario is disallowed - its just an example that <em>may</em> throw an
     *                                 Exception in some systems).
     * @see org.apache.shiro.authz.HostUnauthorizedException
     * @since 1.0
     */
    protected Subject getSubjectBySessionId(Serializable sessionId) throws InvalidSessionException, AuthorizationException {
        Map<String, Object> context = new HashMap<String, Object>(1);
        context.put(SubjectFactory.SESSION_ID, sessionId);
        return getSubject(context);
    }
}