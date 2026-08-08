package com.github.livingwithhippos.unchained.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A cached hoster-matching regex, sourced from TorBox's `webdl/hosters` endpoint (see
 * [Hoster.regex]). Unlike RD, TorBox doesn't distinguish "host" vs "folder" regexes.
 */
@Entity(tableName = "host_regex")
class HostRegex(@PrimaryKey @ColumnInfo(name = "regex") val regex: String)
