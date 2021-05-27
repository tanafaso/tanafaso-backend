use tanafaso;
var usersCount = db.users.count();

var personalChallengesCount = 0;
var challengesCount = 0;
db.users.find({}).forEach(function(user) {
        personalChallengesCount += user.personalChallenges.length;
        challengesCount += user.userChallenges.length;
});

challengesPerUserAverage = 1.0 * challengesCount / usersCount;
personalChallengesPerUserAverage = 1.0 * personalChallengesCount / usersCount;

var friendsPerUserAverage = 0.0;
db.friendships.find({}).forEach(function(friendship) { friendsPerUserAverage += friendship.friends.length; });
friendsPerUserAverage /= usersCount;

var azkarPerChallengeAverage = 0.0;
db.users.find({}).forEach(function(user) {
        user.userChallenges.forEach(function(userChallenge) {
                azkarPerChallengeAverage += userChallenge.subChallenges.length;
        });
});
azkarPerChallengeAverage /= challengesCount;

var averageChallengeLivetime = 0.0;
db.users.find({}).forEach(function(user) {
        user.userChallenges.forEach(function(userChallenge) {
                var expiryDateHours = 1.0 * userChallenge.expiryDate / 60 / 60;
                var createdAtHours = 1.0 * userChallenge.createdAt / 1000 / 60 / 60;
                var diff = expiryDateHours - createdAtHours;
                // Ignore outliers (challenge for more than a month)
                if (diff > 24 * 30) {
                        return;
                }
                averageChallengeLivetime += expiryDateHours - createdAtHours;
        });
});
averageChallengeLivetime /= challengesCount;

print('----- Number of users: ' + usersCount);
print('----- Number of group challenges: ' + db.challenges.count());
print('----- Number of personal challenges: ' + personalChallengesCount);
print('----- Average number of challenges per user: ' + parseInt(challengesPerUserAverage, 10));
print('----- Average number of personal challenges per user: ' + parseInt(personalChallengesPerUserAverage, 10));
print('----- Average number of friends per user: ' + parseInt(friendsPerUserAverage, 10));
print('----- Average number of azkar per challenge: ' + parseInt(azkarPerChallengeAverage, 10));
print('----- Average live time of a challenge in hrs: ' + parseInt(averageChallengeLivetime, 10));