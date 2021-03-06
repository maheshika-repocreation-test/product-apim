/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.rest.integration.tests.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.apimgt.core.models.APIStatus;
import org.wso2.carbon.apimgt.rest.integration.tests.AMIntegrationTestConstants;
import org.wso2.carbon.apimgt.rest.integration.tests.APIMgtBaseIntegrationIT;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.RestAPIException;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.FileInfo;
import org.wso2.carbon.apimgt.rest.integration.tests.util.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.util.SampleTestObjectCreator;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.AMIntegrationTestException;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.APICollectionApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.api.APIIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.API;
import org.wso2.carbon.apimgt.rest.integration.tests.store.api.SubscriptionIndividualApi;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.APIInfo;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.APIList;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.Subscription;
import org.wso2.carbon.apimgt.rest.integration.tests.store.model.SubscriptionList;
import org.wso2.carbon.apimgt.rest.integration.tests.util.TestUtil;

import java.io.File;
import java.util.Collections;

public class APILifeCycleTestCaseIT {

    private API api;
    APICollectionApi apiCollectionApi;
    APIIndividualApi apiIndividualApi;
    SubscriptionIndividualApi subscriptionIndividualApi;

    private static final Logger log = LoggerFactory.getLogger(APILifeCycleTestCaseIT.class);

    @BeforeClass
    public void init() throws AMIntegrationTestException {

        apiCollectionApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APICollectionApi.class);
        apiIndividualApi = TestUtil.getPublisherApiClient("user1", TestUtil.getUser("user1"),
                AMIntegrationTestConstants.DEFAULT_SCOPES).buildClient(APIIndividualApi.class);
    }

    @Test
    public void testCreateApi() {

        api = SampleTestObjectCreator.ApiToCreate("api1-lifecycle", "1.0.0", "/apiLifecycle");
        api = apiCollectionApi.apisPost(api);
        Assert.assertNotNull(api.getId());
    }

    @Test(dependsOnMethods = {"testCreateApi"})
    public void testUpdateApi() {

        api.addPoliciesItem("Gold");
        api.addTransportItem("http");
        api = apiIndividualApi.apisApiIdPut(api.getId(), api, "", "");
    }

    @Test(dependsOnMethods = {"testUpdateApi"})
    public void testUpdateImage() {

        FileInfo fileInfo = apiIndividualApi.apisApiIdThumbnailPost(api.getId(), new File(Thread.currentThread()
                .getContextClassLoader().getResource("img1.jpg").getPath()),null,null);
        apiIndividualApi.apisApiIdThumbnailGet(api.getId(),null,null);

    }

    @Test(dependsOnMethods = {"testUpdateImage"})
    public void testMakeApiProtoType() {

        apiIndividualApi.apisChangeLifecyclePost(APIStatus.PROTOTYPED.getStatus(), api.getId(),
                AMIntegrationTestConstants.DEFAULT_LIFE_CYCLE_CHECK_LIST, "", "");
        Assert.assertEquals(apiIndividualApi.apisApiIdGet(api.getId(), "", "").getLifeCycleStatus(), APIStatus
                .PROTOTYPED.getStatus());
    }

    @Test(dependsOnMethods = {"testMakeApiProtoType"})
    public void testMakeApiPublished() {

        apiIndividualApi.apisChangeLifecyclePost(APIStatus.PUBLISHED.getStatus(), api.getId(),
                AMIntegrationTestConstants.DEFAULT_LIFE_CYCLE_CHECK_LIST, "", "");
        Assert.assertEquals(apiIndividualApi.apisApiIdGet(api.getId(), "", "").getLifeCycleStatus(), APIStatus
                .PUBLISHED.getStatus());
    }

    @Test(dependsOnMethods = {"testMakeApiPublished"})
    public void testCopyApiVersion() {

        apiIndividualApi.apisCopyApiPost("v2.0.0", api.getId());
        org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIList apiList = apiCollectionApi.apisGet(2,
                0, "name:api1-lifecycle", "");
        Assert.assertNotNull(apiList);
        Assert.assertEquals(apiList.getCount().intValue(), 2);
    }

    @Test(dependsOnMethods = {"testCopyApiVersion"})
    public void testMakeApiDeprecated() throws AMIntegrationTestException {

        apiIndividualApi.apisChangeLifecyclePost(APIStatus.DEPRECATED.getStatus(), api.getId(),
                AMIntegrationTestConstants.DEFAULT_LIFE_CYCLE_CHECK_LIST, "", "");
        Assert.assertEquals(apiIndividualApi.apisApiIdGet(api.getId(), "", "").getLifeCycleStatus(), APIStatus
                .DEPRECATED.getStatus());
        org.wso2.carbon.apimgt.rest.integration.tests.store.api.APICollectionApi apiCollectionApi = TestUtil
                .getStoreApiClient("user4", TestUtil.getUser("user4"), AMIntegrationTestConstants.DEFAULT_SCOPES)
                .buildClient(org.wso2.carbon.apimgt.rest.integration.tests.store.api.APICollectionApi.class);
        APIList apiList = apiCollectionApi.apisGet(10, 0, "", "", "");
        Assert.assertNotNull(apiList);
        Assert.assertNotNull(apiList.getList());
        for (APIInfo apiInfo : apiList.getList()) {
            Assert.assertNotEquals(api.getId(), apiInfo.getId());
        }
    }

    @Test(dependsOnMethods = {"testMakeApiDeprecated"})
    public void testMakeApiRetired() {

        apiIndividualApi.apisChangeLifecyclePost(APIStatus.RETIRED.getStatus(), api.getId(),
                AMIntegrationTestConstants.DEFAULT_LIFE_CYCLE_CHECK_LIST, "", "");
    }

    @AfterClass
    public void destroy() {

        org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIList apiList = apiCollectionApi.apisGet(2,
                0, "name:api1-lifecycle", "");
        for (org.wso2.carbon.apimgt.rest.integration.tests.publisher.model.APIInfo apiInfo : apiList.getList()) {
            if (api.getName().equals(apiInfo.getName())) {
                apiIndividualApi.apisApiIdDelete(apiInfo.getId(), "", "");
            }
        }
    }
}
