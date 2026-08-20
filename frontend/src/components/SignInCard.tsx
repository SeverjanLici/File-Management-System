import type { AuthContextProps } from "react-oidc-context";
import { Button } from "./ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "./ui/card";

function SignInCard({ auth }: { auth: AuthContextProps }) {
  return (
    <Card className="min-w-lg">
      <CardHeader>
        <CardTitle>You need to sign in</CardTitle>
        <CardDescription>
          Please sign in to continue to DocPlatform.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Button onClick={() => auth.signinRedirect()}>Sign in</Button>
      </CardContent>
    </Card>
  );
}

export default SignInCard;
