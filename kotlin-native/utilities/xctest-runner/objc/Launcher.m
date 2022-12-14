#import <XCTest/XCTest.h>
#import <xctest_runner/xctest_runner.h>

@interface XCTestLauncher : XCTestCase
@end

@implementation XCTestLauncher

+ (id)defaultTestSuite {
    return [Xctest_runnerMainKt defaultTestSuiteRunner];
}

@end
