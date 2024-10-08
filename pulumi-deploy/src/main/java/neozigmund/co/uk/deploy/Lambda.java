package neozigmund.co.uk.deploy;

import java.util.Map;

import com.pulumi.Pulumi;
import com.pulumi.archive.ArchiveFunctions;
import com.pulumi.archive.inputs.GetFileArgs;
import com.pulumi.asset.FileArchive;
import com.pulumi.aws.iam.IamFunctions;
import com.pulumi.aws.iam.Role;
import com.pulumi.aws.iam.RoleArgs;
import com.pulumi.aws.iam.inputs.GetPolicyDocumentArgs;
import com.pulumi.aws.iam.inputs.GetPolicyDocumentStatementArgs;
import com.pulumi.aws.iam.inputs.GetPolicyDocumentStatementPrincipalArgs;
import com.pulumi.aws.lambda.Function;
import com.pulumi.aws.lambda.FunctionArgs;
import com.pulumi.aws.lambda.inputs.FunctionEnvironmentArgs;

public class Lambda {
	public static void main(String[] args) {
		Pulumi.run(ctx -> {
			final var assumeRole = IamFunctions.getPolicyDocument(GetPolicyDocumentArgs.builder()
					.statements(GetPolicyDocumentStatementArgs.builder().effect("Allow")
							.principals(GetPolicyDocumentStatementPrincipalArgs.builder().type("Service")
									.identifiers("lambda.amazonaws.com").build())
							.actions("sts:AssumeRole").build())
					.build());

			var iamForLambda = new Role("iamForLambda",
					RoleArgs.builder().name("iam_for_lambda")
							.assumeRolePolicy(
									assumeRole.applyValue(getPolicyDocumentResult -> getPolicyDocumentResult.json()))
							.build());

			final var lambda = ArchiveFunctions.getFile(GetFileArgs.builder().type("zip").sourceFile("lambda.js")
					.outputPath("lambda_function_payload.zip").build());

			var testLambda = new Function("testLambda",
					FunctionArgs.builder().code(new FileArchive("lambda_function_payload.zip"))
							.name("lambda_function_name").role(iamForLambda.arn()).handler("index.test")
							.sourceCodeHash(lambda.applyValue(getFileResult -> getFileResult.outputBase64sha256()))
							.runtime("nodejs18.x")
							.environment(FunctionEnvironmentArgs.builder().variables(Map.of("foo", "bar")).build())
							.build());
		});
	}
}
