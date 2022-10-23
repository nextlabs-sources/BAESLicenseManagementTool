$Username='Administrator'
$Password='123next!'|ConvertTo-SecureString -AsPlainText -Force
$Cred=New-Object System.Management.Automation.PSCredential -ArgumentList $Username,$Password
invoke-expression $args[0]