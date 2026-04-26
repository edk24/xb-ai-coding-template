<?php
require __DIR__ . "/../vendor/autoload.php";
use think\file\UploadedFile;

header("Content-Type: application/json");
$f = $_FILES["file"] ?? null;
if (!$f) {
    echo json_encode(["error" => "no file", "files" => $_FILES]);
    exit;
}

try {
    $uf = new UploadedFile($f["tmp_name"], $f["name"], $f["type"], $f["error"]);
    echo json_encode(["success" => true, "name" => $f["name"], "tmp" => $f["tmp_name"], "exists" => is_file($f["tmp_name"]) ? "yes" : "no"]);
} catch (\Throwable $e) {
    echo json_encode(["error" => $e->getMessage(), "tmp" => $f["tmp_name"], "exists" => is_file($f["tmp_name"]) ? "yes" : "no"]);
}
