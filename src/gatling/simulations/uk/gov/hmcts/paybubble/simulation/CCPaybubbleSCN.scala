package uk.gov.hmcts.paybubble.simulation

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class CCPaybubbleSCN extends Simulation {

	val uri02 = "https://idam-web-public.demo.platform.hmcts.net/login"
	val uri03 = "https://paybubble.platform.hmcts.net"
	val uri08 = "https://ccpay-bubble-frontend-demo.service.core-compute-demo.internal"
	val uri10 = "https://app1.pci-pal.com/app_launch.asp"


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
			.get(uri08 + "/api/bulk-scan/cases/1111111111118817")
					.check(jsonPath("$.data.payments[0].dcn_reference").saveAs("dcnNumber"))
							.check(jsonPath("$.data.payments[0].id").saveAs("dcnID"))
							.check(jsonPath("$.data.payments[0].date_banked").saveAs("banked_date"))
							.check(jsonPath("$.data.payments[0].date_created").saveAs("date_created"))
							.check(jsonPath("$.data.payments[0].date_updated").saveAs("date_updated"))))
		.pause(12)
		.exec(http("PB_005_001_AllocateToNewFee")
			.get(uri08 + "/api/fees"))
		.pause(4)
		.exec(http("PB_006_001_FeeSearch")
			.get(uri08 + "/api/fees-jurisdictions/1")
			.resources(http("PB_006_002_FeeSearch")
			.get(uri08 + "/api/fees-jurisdictions/2")))
		.pause(7)
		.exec(http("request_20")
			.post(uri08 + "/api/payment-groups")
			.headers(headers_20)
			.body(StringBody("""{"fees":[{"code":"FEE0120","version":"4","calculated_amount":"550","memo_line":"XXX","natural_account_code":"XXX","ccd_case_number":"1111111111118817","jurisdiction1":"tribunal","jurisdiction2":"upper tribunal lands chamber","description":" Hrg as to entitlement- s84 LoPA1925(e) discharge /modify restrictive convenant ","volume":1,"fee_amount":"550"}]}"""))
			.check(jsonPath("$.data.payment_group_reference").saveAs("pgr_id"))
			.resources(http("request_22")
			.get(uri08 + "/api/payment-history/bulk-scan-feature")
			.headers(headers_9),
            http("request_23")
			.get(uri08 + "/api/payment-history/payment-groups/${pgr_id}"),
            http("request_24")
			.get(uri08 + "/api/bulk-scan/cases/1111111111118817")))
		.pause(3)
		.exec(http("request_25")
			.get(uri08 + "/api/bulk-scan/cases?document_control_number=${dcnNumber}")
			.resources(http("request_26")
			.get(uri08 + "/api/payment-history/cases/1111111111118817/paymentgroups")
				.check(jsonPath("$.payment_groups[0].payments[0].reference").saveAs("payallocatereference"))))
		.pause(15)
		.exec(http("request_27")
			.patch(uri08 + "/api/payment-history/bulk-scan-payments/${dcnNumber}/status/PROCESSED")
			.headers(headers_27)
			.body(StringBody("""PROCESSED"""))
			.resources(http("request_28")
			.post(uri08 + "/api/payment-history/payment-groups/${pgr_id}/bulk-scan-payments")
			.headers(headers_20)
				.body(StringBody("""{"amount":540,"banked_date":"${banked_date}","ccd_case_number":"1111111111118817","exception_record":null,"currency":"GBP","document_control_number":"${dcnNumber}","external_provider":"exela","giro_slip_no":"123456","payment_channel":{"description":"","name":"bulk scan"},"payment_status":{"description":"bulk scan payment completed","name":"success"},"payment_method":"CHEQUE","requestor":"PROBATE","site_id":"AA08"}""")),
            http("request_29")
			.post(uri08 + "/api/payment-history/payment-allocations")
			.headers(headers_20)
							.body(StringBody("""{"payment_allocation_status":{"description":"","name":"Allocated"},"payment_group_reference":"${pgr_id}","payment_reference":"${payallocatereference}","reason":"Incorrect payment received","explanation":"I have put a stop on the case and contacted the applicant requesting the balance of payment","user_name":"Kapil Jain"}""")),
            http("request_30")
			.get(uri08 + "/api/bulk-scan/cases/1111111111118817"),
            http("request_31")
			.get(uri08 + "/api/payment-history/cases/1111111111118817/paymentgroups")))
		.pause(17)
		.exec(http("request_32")
			.get(uri08 + "/api/payment-history/bulk-scan-feature")
			.headers(headers_9)
			.resources(http("request_33")
			.get(uri08 + "/api/payment-history/payment-groups/${pgr_id}")))
		.pause(8)
		.exec(http("request_34")
			.post(uri08 + "/api/payment-history/payment-groups/${pgr_id}/card-payments")
			.headers(headers_20)
			.body(StringBody("""{"currency":"GBP","description":"PayBubble payment","channel":"telephony","provider":"pci pal","ccd_case_number":"1111111111118817","amount":"10.00","service":"DIVORCE","site_id":"AA07"}""")))
		.pause(11)
		.exec(http("request_35")
			.post("/clients/hmcts/payments/launch.php")
			.formParam("orderReference", "RC-1579-6172-1464-6143")
			.formParam("orderAmount", "10.00")
			.formParam("cardHolderName", "")
			.formParam("billingAddress1", "")
			.formParam("billingTown", "")
			.formParam("billingPostcode", "")
			.formParam("ppAccountID", "1210")
			.formParam("renderMethod", "HTML")
			.formParam("apiKey", "3ecfe793e7153715345a0ec4fe536699")
			.formParam("callbackURL", "https://ip3cloud.com/clients/hmcts/payments/data/callback.php")
			.formParam("callbackURLTwo", "https://core-api-mgmt-demo.azure-api.net/telephony-api/telephony/callback")
			.formParam("redirectURL", "")
			.formParam("hosted", "true")
			.formParam("transactionType", "SAL")
			.resources(http("request_39")
			.get(uri10 + "?UseSessionPIN=y&u=55753312&p=0f8H6dkMAPqS&ORDERID=RC-1579-6172-1464-6143&CN=&OWNERADDRESS=&OWNERZIP=&OWNERTOWN=&opccprocessamount=1000&OPERATION=SAL&ppAccountID=1210&customData3=&pciWebID=SESSION-20200121143346-8416&callbackURL=https%253A%252F%252Fip3cloud.com%252Fclients%252Fhmcts%252Fpayments%252Fdata%252Fcallback.php&callbackURLTwo=https%253A%252F%252Fcore-api-mgmt-demo.azure-api.net%252Ftelephony-api%252Ftelephony%252Fcallback&customData1=RC-1579-6172-1464-614320200121143346")))
		.pause(2)
		.pause(6)
		.exec(http("request_161")
			.get(uri03 + "/?")
			.check(status.is(200)))

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}