package com.github.nacabaro.vbhelper.battle

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SpriteFileManager(private val context: Context) {
    
    fun copySpriteFilesToInternalStorage() {
        try {
            println("Starting sprite file copy process...")
            
            // Debug: List what's in the assets directory
            val assetManager = context.assets
            val battleSpritesFiles = assetManager.list("battle_sprites")
            println("battle_sprites directory in assets contains: ${battleSpritesFiles?.joinToString(", ")}")
            
            val extractedAssetsFiles = assetManager.list("battle_sprites/extracted_assets")
            println("battle_sprites/extracted_assets directory in assets contains: ${extractedAssetsFiles?.joinToString(", ")}")
            
            // Check specifically for extracted_atksprites in assets (now directly under battle_sprites)
            val atkspritesInAssets = assetManager.list("battle_sprites/extracted_atksprites")
            println("extracted_atksprites in assets contains: ${atkspritesInAssets?.size ?: 0} files")
            if (atkspritesInAssets != null && atkspritesInAssets.isNotEmpty()) {
                println("First few attack files in assets: ${atkspritesInAssets.take(5).joinToString(", ")}")
            }
            
            // Check for extracted_battlebgs in assets (now directly under battle_sprites)
            val battlebgsInAssets = assetManager.list("battle_sprites/extracted_battlebgs")
            println("extracted_battlebgs in assets contains: ${battlebgsInAssets?.size ?: 0} files")
            if (battlebgsInAssets != null && battlebgsInAssets.isNotEmpty()) {
                println("First few battle background files in assets: ${battlebgsInAssets.take(5).joinToString(", ")}")
            }
            
            // Try to list all possible subdirectories in battle_sprites
            println("Checking all possible subdirectories in battle_sprites...")
            battleSpritesFiles?.forEach { subdir ->
                try {
                    val subdirFiles = assetManager.list("battle_sprites/$subdir")
                    println("  $subdir contains: ${subdirFiles?.size ?: 0} files")
                    if (subdirFiles != null && subdirFiles.isNotEmpty()) {
                        println("    First few files: ${subdirFiles.take(3).joinToString(", ")}")
                    }
                } catch (e: Exception) {
                    println("  Error listing $subdir: ${e.message}")
                }
            }
            
            // Create the base directory for battle_sprites
            val battleSpritesDir = File(context.filesDir, "battle_sprites")
            if (!battleSpritesDir.exists()) {
                battleSpritesDir.mkdirs()
                println("Created battle_sprites directory: ${battleSpritesDir.absolutePath}")
            } else {
                println("battle_sprites directory already exists: ${battleSpritesDir.absolutePath}")
            }
            
            // Copy all subdirectories from battle_sprites assets to internal storage
            println("Copying all battle_sprites subdirectories...")
            battleSpritesFiles?.forEach { subdir ->
                val sourcePath = "battle_sprites/$subdir"
                val targetDir = File(battleSpritesDir, subdir)
                println("Copying $sourcePath to ${targetDir.absolutePath}")
                copyAssetDirectory(sourcePath, targetDir)
            }
            
            println("Sprite files copied successfully to: ${battleSpritesDir.absolutePath}")
            
            // Verify that attack sprites were copied
            val atkspritesDir = File(battleSpritesDir, "extracted_atksprites")
            if (atkspritesDir.exists()) {
                val attackFiles = atkspritesDir.listFiles()
                println("Attack sprites directory exists with ${attackFiles?.size ?: 0} files")
                if (attackFiles != null && attackFiles.isNotEmpty()) {
                    println("First few attack files: ${attackFiles.take(5).map { it.name }}")
                }
            } else {
                println("WARNING: extracted_atksprites directory does not exist!")
                // List what's actually in the battle_sprites directory
                val battleSpritesContents = battleSpritesDir.listFiles()
                println("battle_sprites directory contains: ${battleSpritesContents?.map { it.name }?.joinToString(", ")}")
            }
            
            // Verify that battle backgrounds were copied
            val battlebgsDir = File(battleSpritesDir, "extracted_battlebgs")
            if (battlebgsDir.exists()) {
                val bgFiles = battlebgsDir.listFiles()
                println("Battle backgrounds directory exists with ${bgFiles?.size ?: 0} files")
                if (bgFiles != null && bgFiles.isNotEmpty()) {
                    println("First few battle background files: ${bgFiles.take(5).map { it.name }}")
                }
            } else {
                println("WARNING: extracted_battlebgs directory does not exist!")
            }
            
        } catch (e: Exception) {
            println("Error copying sprite files: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun copyAssetDirectory(assetPath: String, targetDir: File) {
        try {
            val assetManager = context.assets
            val files = assetManager.list(assetPath) ?: return
            
            println("Copying asset directory: $assetPath (${files.size} items)")
            println("Files found: ${files.joinToString(", ")}")
            
            for (file in files) {
                val assetFilePath = if (assetPath.isEmpty()) file else "$assetPath/$file"
                val targetFile = File(targetDir, file)
                
                // Create subdirectories if needed
                if (targetFile.parentFile != null && !targetFile.parentFile!!.exists()) {
                    targetFile.parentFile!!.mkdirs()
                }
                
                // Check if it's a directory by trying to list its contents
                try {
                    val subFiles = assetManager.list(assetFilePath)
                    if (subFiles != null && subFiles.isNotEmpty()) {
                        // It's a directory, create it and copy contents
                        println("Copying subdirectory: $assetFilePath (${subFiles.size} files)")
                        if (!targetFile.exists()) {
                            targetFile.mkdirs()
                        }
                        copyAssetDirectory(assetFilePath, targetFile)
                    } else {
                        // It's a file, copy it
                        copyAssetFile(assetFilePath, targetFile)
                    }
                } catch (e: Exception) {
                    // If we can't list contents, it's probably a file
                    println("Treating $assetFilePath as file (could not list contents)")
                    copyAssetFile(assetFilePath, targetFile)
                }
            }
            
            // Special handling for extracted_atksprites - try to copy it directly if it wasn't found
            if (assetPath == "battle_sprites/extracted_assets") {
                println("Special handling: Checking for extracted_atksprites directory...")
                try {
                    val atkspritesFiles = assetManager.list("battle_sprites/extracted_assets/extracted_atksprites")
                    if (atkspritesFiles != null && atkspritesFiles.isNotEmpty()) {
                        println("Found extracted_atksprites with ${atkspritesFiles.size} files")
                        val atkspritesDir = File(targetDir, "extracted_atksprites")
                        if (!atkspritesDir.exists()) {
                            atkspritesDir.mkdirs()
                        }
                        copyAssetDirectory("battle_sprites/extracted_assets/extracted_atksprites", atkspritesDir)
                    } else {
                        println("extracted_atksprites directory not found in assets")
                    }
                } catch (e: Exception) {
                    println("Error checking extracted_atksprites: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            println("Error copying asset directory $assetPath: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun copyAssetFile(assetPath: String, targetFile: File) {
        try {
            val inputStream = context.assets.open(assetPath)
            val outputStream = FileOutputStream(targetFile)
            
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            
            println("Copied: $assetPath -> ${targetFile.absolutePath}")
        } catch (e: IOException) {
            println("Error copying asset file $assetPath: ${e.message}")
        }
    }
    
    fun checkSpriteFilesExist(): Boolean {
        val battleSpritesDir = File(context.filesDir, "battle_sprites")
        val extractedAssetsDir = File(battleSpritesDir, "extracted_assets")
        val extractedStatsDir = File(battleSpritesDir, "extracted_digimon_stats")
        val atkspritesDir = File(battleSpritesDir, "extracted_atksprites")
        val battlebgsDir = File(battleSpritesDir, "extracted_battlebgs")
        
        val battleSpritesExist = battleSpritesDir.exists() && battleSpritesDir.listFiles()?.isNotEmpty() == true
        val assetsExist = extractedAssetsDir.exists() && extractedAssetsDir.listFiles()?.isNotEmpty() == true
        val statsExist = extractedStatsDir.exists() && extractedStatsDir.listFiles()?.isNotEmpty() == true
        val atkspritesExist = atkspritesDir.exists() && atkspritesDir.listFiles()?.isNotEmpty() == true
        val battlebgsExist = battlebgsDir.exists() && battlebgsDir.listFiles()?.isNotEmpty() == true
        
        println("Checking sprite files exist:")
        println("  battle_sprites exists: $battleSpritesExist")
        println("  extracted_assets exists: $assetsExist")
        println("  extracted_digimon_stats exists: $statsExist")
        println("  extracted_atksprites exists: $atkspritesExist")
        println("  extracted_battlebgs exists: $battlebgsExist")
        
        return battleSpritesExist && assetsExist && statsExist && atkspritesExist && battlebgsExist
    }
    
    fun clearSpriteFiles() {
        try {
            val battleSpritesDir = File(context.filesDir, "battle_sprites")
            
            if (battleSpritesDir.exists()) {
                deleteDirectory(battleSpritesDir)
                println("Cleared battle_sprites directory")
            }
            
        } catch (e: Exception) {
            println("Error clearing sprite files: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun deleteDirectory(directory: File) {
        if (directory.exists()) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        deleteDirectory(file)
                    } else {
                        file.delete()
                    }
                }
            }
            directory.delete()
        }
    }
} 