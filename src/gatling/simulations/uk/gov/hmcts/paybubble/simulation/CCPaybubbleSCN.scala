package uk.gov.hmcts.paybubble.simulation

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class CCPaybubbleSCN extends Simulation {

	val uri02 = "https://idam-web-public.demo.platform.hmcts.net/login"
	val uri03 = "https://paybubble.platform.hmcts.net"
	val uri08 = "https://ccpay-bubble-frontend-demo.service.core-compute-demo.internal"


	val httpProtocol = http
		.baseUrl("https://ip3cloud.com")
		.proxy(Proxy("proxyout.reform.hmcts.net", 8080))

	val headers_9 = Map(
		"Accept" -> "application/json, text/plain, */*",
		"CSRF-Token" -> "${CTOKEN}",
		"Sec-Fetch-Mode" -> "cors",
		"Sec-Fetch-Site" -> "same-origin")

	val headers_20 = Map(
		"Accept" -> "application/json, text/plain, */*",
		"CSRF-Token" -> "${CTOKEN}",
		"Content-Type" -> "application/json",
		"Origin" -> uri08,
		"Sec-Fetch-Mode" -> "cors",
		"Sec-Fetch-Site" -> "same-origin")

	val headers_27 = Map(
		"Accept" -> "application/json, text/plain, */*",
		"CSRF-Token" -> "${CTOKEN}",
		"Content-Type" -> "text/plain",
		"Origin" -> uri08,
		"Sec-Fetch-Mode" -> "cors",
		"Sec-Fetch-Site" -> "same-origin")


	val scn = scenario("RecordedSimulation")
		.exec(http("PB_001_001_Landingpage")
			.get(uri08 + "/")
			.check(regex(""".*state=(.*)&amp;client_id""").saveAs("State")))

		.exec(http("PB_002_001_LoginPage")
			.get(uri02 + "?response_type=code&state=${State}&client_id=paybubble&redirect_uri=" + uri08 +"/oauth2/callback")
			.check(css("input[name='_csrf']", "value").saveAs("csrftoken")))
		.pause(5)

		.exec(http("PB_003_001_SubmitLogin")
			.post(uri02 + "?response_type=code&state=${State}&client_id=paybubble&redirect_uri=" + uri08 +"/oauth2/callback")
			.formParam("username", "robreallywantsccdaccess@mailinator.com")
			.formParam("password", "Testing1234")
			.formParam("save", "Sign in")
			.formParam("selfRegistrationEnabled", "false")
			.formParam("_csrf", "${csrftoken}")
			.check(regex(""".*content="(.*)"><title>""").saveAs("CTOKEN"))
			.resources(http("PB_003_002_SubmitLogin")
				.get(uri08 + "/assets/fonts/light-f38ad40456-v1.woff2"),
				http("PB_003_003_SubmitLogin")
					.get(uri08 + "/v1-f38ad40456-light.f38ad40456df126d7536.woff2?0.24.1"),
				http("PB_003_004_SubmitLogin")
					.get(uri08 + "/open-government-licence_2x.154aaeec20e181e607c0.png?0.24.1"),
				http("PB_003_005_SubmitLogin")
					.get(uri08 + "/assets/fonts/bold-a2452cb66f-v1.woff2"),
				http("PB_003_006_SubmitLogin")
					.get(uri08 + "/api/payment-history/bulk-scan-feature")
					.headers(headers_9),
				http("PB_003_007_SubmitLogin")
					.get(uri08 + "/v1-a2452cb66f-bold.a2452cb66fcc8cd179b5.woff2?0.24.1"),
				http("PB_003_008_SubmitLogin")
					.get(uri08 + "/api/payment-history/bulk-scan-feature")
					.headers(headers_9)))
		.pause(10)
		.exec(http("PB_004_001_SearchCase")
			.get(uri08 + "/api/cases/1111111111118817")
			.headers(headers_9)
			.resources(http("PB_004_002_SearchCase_paymentgroups")
			.get(uri08 + "/api/payment-history/cases/1111111111118817/paymentgroups"),
            http("PB_004_003_SearchCase")
			.get(uri08 + "/api/bulk-scan/cases/1111111111118817")))
					.pause(11)
		.exec(http("request_161")
			.get(uri03 + "/?")
			.check(status.is(200)))

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}