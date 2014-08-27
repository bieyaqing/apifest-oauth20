/*
 * Copyright 2013-2014, ApiFest project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apifest.oauth20;

import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Rossitsa Borissova
 */
public class ScopeServiceTest {

    ScopeService service;

    @BeforeMethod
    public void setup() {
        service = spy(new ScopeService());
        MockDBManagerFactory.install();
    }

    @Test
    public void when_client_id_query_param_invoke_get_scopes_by_clientId() throws Exception {
        // GIVEN
        String clientId = "826064099791766";
        HttpRequest req = mock(HttpRequest.class);
        willReturn("http://localhost:8080/oauth20/scope?client_id=" + clientId).given(req).getUri();
        willReturn(null).given(service).getScopes(clientId);

        // WHEN
        service.getScopes(req);

        // THEN
        verify(service).getScopes(clientId);
    }

    @Test
    public void when_no_client_id_query_param_get_all_scopes() throws Exception {
        // GIVEN
        HttpRequest req = mock(HttpRequest.class);
        willReturn("http://localhost:8080/oauth20/scope").given(req).getUri();

        // WHEN
        service.getScopes(req);

        // THEN
        verify(service, never()).getScopes(anyString());
    }

    @Test
    public void when_get_all_scopes_invoke_load_all_scopes() throws Exception {
        // GIVEN
        HttpRequest req = mock(HttpRequest.class);
        willReturn("http://localhost:8080/oauth20/scope").given(req).getUri();

        // WHEN
        service.getScopes(req);

        // THEN
        verify(DBManagerFactory.dbManager).getAllScopes();
    }

    @Test
    public void when_get_client_scopes_invoke_scopes_for_that_client_only() throws Exception {
        // GIVEN
        String clientId = "826064099791766";
        HttpRequest req = mock(HttpRequest.class);
        willReturn("http://localhost:8080/oauth20/scope?client_id=" + clientId).given(req).getUri();
        String scope = "basic";
        ClientCredentials creds = new ClientCredentials("appName", scope, "descr", "http://example.com");
        willReturn(creds).given(DBManagerFactory.dbManager).findClientCredentials(clientId);

        // WHEN
        service.getScopes(req);

        // THEN
        verify(DBManagerFactory.dbManager).findClientCredentials(clientId);
        verify(DBManagerFactory.dbManager).findScope("basic");
    }

    @Test
    public void when_client_has_several_scopes_invoke_get_scope_details_for_each_scope()
            throws Exception {
        // GIVEN
        String clientId = "826064099791766";
        HttpRequest req = mock(HttpRequest.class);
        willReturn("http://localhost:8080/oauth20/scope?client_id=" + clientId).given(req).getUri();
        String scope = "basic extended";
        ClientCredentials creds = new ClientCredentials("appName", scope, "descr", "http://example.com");
        willReturn(creds).given(DBManagerFactory.dbManager).findClientCredentials(clientId);

        // WHEN
        service.getScopes(req);

        // THEN
        verify(DBManagerFactory.dbManager).findClientCredentials(clientId);
        verify(DBManagerFactory.dbManager).findScope("basic");
        verify(DBManagerFactory.dbManager).findScope("extended");
    }

    @Test
    public void when_scope_is_null_get_client_app_scope() throws Exception {
        // GIVEN
        String clientId = "826064099791766";
        MockDBManagerFactory.install();
        ClientCredentials creds = mock(ClientCredentials.class);
        willReturn("basic").given(creds).getScope();
        willReturn(creds).given(DBManagerFactory.getInstance()).findClientCredentials(clientId);

        // WHEN
        String scope = service.getValidScope(null, clientId);

        // THEN
        assertEquals(scope, "basic");
    }

    @Test
    public void when_scope_is_valid_check_client_app_scope_contains_it() throws Exception {
        // GIVEN
        String clientId = "826064099791766";
        MockDBManagerFactory.install();
        ClientCredentials creds = mock(ClientCredentials.class);
        willReturn("extended basic").given(creds).getScope();
        willReturn(creds).given(DBManagerFactory.getInstance()).findClientCredentials(clientId);

        // WHEN
        String scope = service.getValidScope("basic", clientId);

        // THEN
        assertEquals(scope, "basic");
    }

    @Test
    public void when_scope_is_not_contained_in_client_app_scope_return_null() throws Exception {
        // GIVEN
        String clientId = "826064099791766";
        MockDBManagerFactory.install();
        ClientCredentials creds = mock(ClientCredentials.class);
        willReturn("basic").given(creds).getScope();
        willReturn(creds).given(DBManagerFactory.getInstance()).findClientCredentials(clientId);

        // WHEN
        String scope = service.getValidScope("extended", clientId);

        // THEN
        assertNull(scope);
    }

    @Test
    public void when_scope_is_contained_return_true() throws Exception {
        // GIVEN
        String scope = "extended";
        String scopeList = "basic extended";

        // WHEN
        boolean allowed = service.scopeAllowed(scope, scopeList);

        // THEN
        assertTrue(allowed);
    }

    @Test
    public void when_scope_is_not_contained_return_false() throws Exception {
        // GIVEN
        String scope = "payment";
        String scopeList = "basic,extended";

        // WHEN
        boolean allowed = service.scopeAllowed(scope, scopeList);

        // THEN
        assertFalse(allowed);
    }

    @Test
    public void when_scope_contains_two_scopes_check_all() throws Exception {
        // GIVEN
        String scope = "basic extended";
        String scopeList = "basic extended";

        // WHEN
        boolean allowed = service.scopeAllowed(scope, scopeList);

        // THEN
        assertTrue(allowed);
    }

    @Test
    public void when_scope_with_CC_expires_in_900_return_900() throws Exception {
        // GIVEN
        String scopeName = "basic";
        Scope scope = new Scope();
        scope.setScope(scopeName);
        scope.setCcExpiresIn(900);
        scope.setPassExpiresIn(300);
        List<Scope> loadedScope = new ArrayList<Scope>();
        loadedScope.add(scope);
        willReturn(loadedScope).given(service).loadScopes(scopeName);

        // WHEN
        int result = service.getExpiresIn("client_credentials", scopeName);

        // THEN
        assertEquals(result, 900);
    }

    @Test
    public void when_scope_with_PASS_expires_in_300_return_300() throws Exception {
        // GIVEN
        String scopeName = "basic";
        Scope scope = new Scope();
        scope.setScope(scopeName);
        scope.setCcExpiresIn(900);
        scope.setPassExpiresIn(300);
        List<Scope> loadedScope = new ArrayList<Scope>();
        loadedScope.add(scope);
        willReturn(loadedScope).given(service).loadScopes(scopeName);

        // WHEN
        int result = service.getExpiresIn("password", scopeName);

        // THEN
        assertEquals(result, 300);
    }

    @Test
    public void when_several_scopes_and_pass_return_min_pass_expires_in() throws Exception {
        // GIVEN
        String scopeName = "basic extended";
        List<Scope> loadedScope = new ArrayList<Scope>();

        Scope scope1 = new Scope();
        scope1.setScope("basic");
        scope1.setCcExpiresIn(900);
        scope1.setPassExpiresIn(300);
        loadedScope.add(scope1);

        Scope scope2 = new Scope();
        scope2.setScope("extended");
        scope2.setCcExpiresIn(600);
        scope2.setPassExpiresIn(180);
        loadedScope.add(scope2);

        willReturn(loadedScope).given(service).loadScopes(scopeName);

        // WHEN
        int result = service.getExpiresIn("password", scopeName);

        // THEN
        assertEquals(result, 180);
    }

    @Test
    public void when_several_scopes_and_CC_return_min_CC_expires_in() throws Exception {
        // GIVEN
        String scopeName = "basic extended";
        List<Scope> loadedScope = new ArrayList<Scope>();

        Scope scope1 = new Scope();
        scope1.setScope("basic");
        scope1.setCcExpiresIn(900);
        scope1.setPassExpiresIn(300);
        loadedScope.add(scope1);

        Scope scope2 = new Scope();
        scope2.setScope("extended");
        scope2.setCcExpiresIn(600);
        scope2.setPassExpiresIn(180);
        loadedScope.add(scope2);

        willReturn(loadedScope).given(service).loadScopes(scopeName);

        // WHEN
        int result = service.getExpiresIn("client_credentials", scopeName);

        // THEN
        assertEquals(result, 600);
    }

    @Test
    public void when_no_loaded_scopes_and_CC_set_default_CC_expires_in() throws Exception {
        // GIVEN
        String scopeName = "not_existing";
        willReturn(Collections.EMPTY_LIST).given(service).loadScopes(scopeName);

        // WHEN
        int result = service.getExpiresIn("client_credentials", scopeName);

        // THEN
        assertEquals(result, OAuthServer.DEFAULT_CC_EXPIRES_IN);
    }

    @Test
    public void when_no_loaded_scopes_and_PASS_set_default_PASS_expires_in() throws Exception {
        // GIVEN
        String scopeName = "not_existing";
        willReturn(Collections.EMPTY_LIST).given(service).loadScopes(scopeName);

        // WHEN
        int result = service.getExpiresIn("password", scopeName);

        // THEN
        assertEquals(result, OAuthServer.DEFAULT_PASSWORD_EXPIRES_IN);
    }
}
