<?php
$pdo=new PDO('mysql:host=localhost;dbname=rssi_db;charset=utf8','root', '');
if(isset($_REQUEST['C2rssi']))
{
	$C2rssi = $_REQUEST['C2rssi'];

	$sql=$pdo->prepare('insert into mac_30aea40894c2_table values(?)');

	if ($sql->execute([$C2rssi])) 
	{
		echo 'C2rssi success。';
	} 
	else 
	{
		echo 'C2rssi failed';
	}
}
if(isset($_REQUEST['BArssi']))
{
	$BArssi = $_REQUEST['BArssi'];

	$sql=$pdo->prepare('insert into mac_30aea40891ba_table values(?)');

	if ($sql->execute([$BArssi])) 
	{
		echo 'BArssi success';
	} 
	else 
	{
		echo 'BArssi failed';
	}
}

if(isset($_REQUEST['AArssi']))
{
	$AArssi = $_REQUEST['AArssi'];

	$sql=$pdo->prepare('insert into mac_aaaaaaaaaaaa_table values(?)');

	if ($sql->execute([$AArssi])) 
	{
		echo 'AArssi success。';
	} 
	else 
	{
		echo 'AArssi failed';
	}
}


?>

