package com.github.livingwithhippos.unchained

import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.torrentdetails.model.getFilesNodes
import com.github.livingwithhippos.unchained.utilities.Node
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.junit.Test

/** This file is used to avoid errors with `gradlew lint test` */
class TestTorrentFileParser {

    private val moshi: Moshi = Moshi.Builder().build()
    private val jsonAdapter: JsonAdapter<TorrentItem> = moshi.adapter(TorrentItem::class.java)

    // A trimmed-down `torrents/mylist` single-item response (TorBox's shape, see
    // https://api-docs.torbox.app/). Keeps a couple of top-level files plus a nested "mars/"
    // folder so [getFilesNodes] still has a directory to build.
    @Test
    fun torrentItemNodes() {
        val json =
            """
                {
                    "id": 42,
                    "auth_id": "abc123",
                    "hash": "7f53b1ae54fe80b6c98b4e263e59f5b08061000c",
                    "name": "uc-berkeley-cs61c-great-ideas-in-computer-architecture",
                    "magnet": "magnet:?xt=urn:btih:7f53b1ae54fe80b6c98b4e263e59f5b08061000c",
                    "size": 748852727,
                    "active": true,
                    "created_at": "2022-10-31T10:40:30.000Z",
                    "download_state": "downloading",
                    "seeds": 4,
                    "peers": 2,
                    "progress": 0.42,
                    "download_speed": 1141000,
                    "eta": 600,
                    "download_present": false,
                    "download_finished": false,
                    "files": [
                        {
                            "id": 1,
                            "name": "01-course-introduction.mp3",
                            "short_name": "01-course-introduction.mp3",
                            "absolute_path": "/completed/7f53b1ae/uc-berkeley-cs61c-great-ideas-in-computer-architecture/01-course-introduction.mp3",
                            "size": 11440065,
                            "mimetype": "audio/mpeg"
                        },
                        {
                            "id": 2,
                            "name": "01-course-introduction.pdf",
                            "short_name": "01-course-introduction.pdf",
                            "absolute_path": "/completed/7f53b1ae/uc-berkeley-cs61c-great-ideas-in-computer-architecture/01-course-introduction.pdf",
                            "size": 533998,
                            "mimetype": "application/pdf"
                        },
                        {
                            "id": 3,
                            "name": "README.txt",
                            "short_name": "README.txt",
                            "absolute_path": "/completed/7f53b1ae/uc-berkeley-cs61c-great-ideas-in-computer-architecture/README.txt",
                            "size": 62,
                            "mimetype": "text/plain"
                        },
                        {
                            "id": 4,
                            "name": "mars/mars.jar",
                            "short_name": "mars.jar",
                            "absolute_path": "/completed/7f53b1ae/uc-berkeley-cs61c-great-ideas-in-computer-architecture/mars/mars.jar",
                            "size": 4169142,
                            "mimetype": "application/java-archive"
                        },
                        {
                            "id": 5,
                            "name": "mars/mips.asm",
                            "short_name": "mips.asm",
                            "absolute_path": "/completed/7f53b1ae/uc-berkeley-cs61c-great-ideas-in-computer-architecture/mars/mips.asm",
                            "size": 145,
                            "mimetype": "text/plain"
                        }
                    ],
                    "cached": true,
                    "expires_at": null
                }
            """
                .trimIndent()
        val item: TorrentItem = jsonAdapter.fromJson(json)!!

        val torrentStructure: Node<TorrentFileItem> = getFilesNodes(item)

        var newValue = false
        Node.traverseNodeDepthFirst(torrentStructure) {
            if (it.value.id == 2L) {
                newValue = !it.value.selected
                it.value.selected = newValue
            }
        }
        Node.traverseDepthFirst(torrentStructure) {
            if (it.id == 2L) {
                assert(it.selected == newValue)
            }
        }
    }
}
