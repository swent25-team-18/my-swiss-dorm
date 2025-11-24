package com.android.mySwissDorm.model.chat.requestedmessage

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Provides a singleton instance of RequestedMessageRepository. Similar to other repository
 * providers in the app.
 */
object RequestedMessageRepositoryProvider {
  var repository: RequestedMessageRepository =
      RequestedMessageRepositoryFirestore(FirebaseFirestore.getInstance())
}
